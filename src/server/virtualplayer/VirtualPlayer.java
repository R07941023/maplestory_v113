package server.virtualplayer;

import client.MapleCharacter;
import handling.channel.ChannelServer;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.PartyOperation;
import handling.world.World;
import server.life.MapleMonster;
import server.maps.MapleMap;
import server.movement.LifeMovementFragment;
import server.movement.StaticLifeMovement;
import tools.AttackPair;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.packet.MobPacket;

import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a server-controlled virtual player (bot).
 * Handles AI logic for combat (auto-attack monsters) and following owner to different maps.
 */
public class VirtualPlayer {

    public enum State {
        IDLE,
        FOLLOWING,
        ATTACKING
    }

    private final MapleCharacter character;
    private MapleCharacter owner; // The GM who spawned this bot
    private State currentState = State.IDLE;

    // AI settings
    private static final int ATTACK_RANGE_MIN = 1;
    private static final int ATTACK_RANGE_MAX = 30;
    private static final int SEARCH_RANGE = 6000; // pixels to search for monsters (X distance)
    private static final int SAME_PLATFORM_Y = 60; // max Y difference to consider same platform
    private static final long ATTACK_COOLDOWN = 1000; // ms between attacks
    private static final long MOVE_COOLDOWN = 200;   // ms between movements
    private static final int WALK_SPEED = 125;        // pixels per second (standard walk speed)

    private int attackRange = ATTACK_RANGE_MIN + (int) (Math.random() * (ATTACK_RANGE_MAX - ATTACK_RANGE_MIN + 1));
    private long lastAttackTime = 0;
    private long lastMoveTime = 0;
    private long lastYSyncTime = 0;
    private long lastExpressionTime = 0;
    private static final long Y_SYNC_INTERVAL = 5000;
    private static final long EXPRESSION_INTERVAL = 60000; // ms between random expressions
    private boolean active = false;

    // Adaptive tick: when idle, only process every IDLE_TICK_SKIP ticks (= 1000ms)
    private static final int IDLE_TICK_SKIP = 5;
    private int idleTickCounter = 0;

    public VirtualPlayer(MapleCharacter character, MapleCharacter owner, int channel, int world) {
        this.character = character;
        this.owner = owner;
    }

    public MapleCharacter getCharacter() {
        return character;
    }

    public MapleCharacter getOwner() {
        return owner;
    }

    public void setOwner(MapleCharacter owner) {
        this.owner = owner;
    }

    public State getCurrentState() {
        return currentState;
    }

    public boolean isActive() {
        return active;
    }

    /**
     * Activate the virtual player and add to the map
     */
    public void activate(int channel, int mapId) {
        if (active) return;

        active = true;
        ChannelServer cs = ChannelServer.getInstance(channel);
        if (cs == null) return;

        MapleMap map = cs.getMapFactory().getMap(mapId);
        if (map == null) return;

        character.setMap(map);
        Point spawnPos = owner != null ? owner.getPosition() : map.getPortal(0).getPosition();
        character.setPosition(snapToGround(map, spawnPos));
        map.addPlayer(character);
    }

    /**
     * Deactivate the virtual player and remove from map
     */
    public void deactivate() {
        if (!active) return;

        active = false;

        // Leave party if in one
        if (character.getParty() != null) {
            World.Party.updateParty(character.getParty().getId(), PartyOperation.LEAVE,
                new MaplePartyCharacter(character));
            character.setParty(null);
        }

        // Remove from map
        if (character.getMap() != null) {
            character.getMap().removePlayer(character);
        }

        // Save character
        character.saveToDB(false, false);
    }

    /**
     * Main AI tick - called by VirtualPlayerManager every 200ms.
     * Idle bots are skipped every IDLE_TICK_SKIP ticks (= 1000ms effective rate).
     */
    void tick() {
        if (!active || character.getMap() == null) return;

        // Adaptive rate: skip ticks when idle to reduce CPU load
        if (currentState == State.IDLE) {
            if (++idleTickCounter < IDLE_TICK_SKIP) return;
            idleTickCounter = 0;
        } else {
            idleTickCounter = 0;
        }

        long currentTime = System.currentTimeMillis();

        // Priority 1: Follow owner to different map
        if (shouldFollowToNewMap()) {
            changeMapToOwner();
            return;
        }

        // Random facial expression every minute
        if (currentTime - lastExpressionTime >= EXPRESSION_INTERVAL) {
            lastExpressionTime = currentTime;
            randomExpression();
        }

        // Priority 2: Re-sync Y axis if bot is on wrong platform (every 5s)
        if (currentTime - lastYSyncTime >= Y_SYNC_INTERVAL) {
            lastYSyncTime = currentTime;
            if (shouldSyncY()) {
                syncYToOwner();
                return;
            }
        }

        // Priority 2: Attack nearby monsters (if in range)
        if (shouldAttack(currentTime)) {
            attackNearbyMonster();
            return;
        }

        // Priority 3: Move towards monsters to attack
        if (shouldMoveToMonster(currentTime)) {
            moveTowardsMonster();
        }
    }

    private static final int FLOOR_CHANGE_Y = 120; // Y diff to consider owner changed floor

    private boolean shouldSyncY() {
        if (owner == null || owner.getMapId() != character.getMapId()) return false;
        return Math.abs(character.getPosition().y - owner.getPosition().y) > FLOOR_CHANGE_Y;
    }

    private void syncYToOwner() {
        MapleMap map = character.getMap();
        if (map == null) return;

        Point groundPos = snapToGround(map, new Point(character.getPosition().x, owner.getPosition().y));
        map.removePlayer(character);
        character.setPosition(groundPos);
        map.addPlayer(character);
    }

    // ==================== Utility ====================

    /**
     * Given a position, find the foothold below it and return a grounded point.
     * Also updates the character's fh so it doesn't float in mid-air.
     */
    private void randomExpression() {
        MapleMap map = character.getMap();
        if (map == null) return;
        int expression = (int) (Math.random() * 8);
        map.broadcastMessage(MaplePacketCreator.facialExpression(character, expression));
    }

    private Point snapToGround(MapleMap map, Point pos) {
        if (map.getFootholds() == null) return pos;
        server.maps.MapleFoothold fh = map.getFootholds().findBelow(pos);
        if (fh == null) return pos;
        character.setFh(fh.getId());
        return new Point(pos.x, fh.getY1());
    }

    // ==================== Follow Owner to Map ====================

    private boolean shouldFollowToNewMap() {
        if (owner == null || !owner.isAlive()) return false;
        return character.getMapId() != owner.getMapId();
    }

    private void changeMapToOwner() {
        if (owner == null) return;

        MapleMap targetMap = owner.getMap();
        if (targetMap == null) return;

        Point groundPos = snapToGround(targetMap, targetMap.getPortal(0).getPosition());
        character.setPosition(groundPos);
        character.changeMap(targetMap, targetMap.getPortal(0));
    }

    // ==================== Movement AI ====================

    private boolean shouldMoveToMonster(long currentTime) {
        if (currentTime - lastMoveTime < MOVE_COOLDOWN) return false;
        if (currentTime - lastAttackTime < ATTACK_COOLDOWN) return false; // 攻擊動畫期間不移動
        if (!character.isAlive()) return false;

        // Find a monster to move towards (same platform only)
        MapleMonster monster = findMonsterOnPlatform(SEARCH_RANGE);
        if (monster == null) return false;

        // Check if we're already in attack range (X distance only)
        int xDist = Math.abs(character.getPosition().x - monster.getPosition().x);
        return xDist > attackRange;
    }

    private void moveTowardsMonster() {
        MapleMonster monster = findMonsterOnPlatform(SEARCH_RANGE);
        if (monster == null) return;

        lastMoveTime = System.currentTimeMillis();
        currentState = State.FOLLOWING;

        Point monsterPos = monster.getPosition();
        Point current = character.getPosition();

        // Use owner's Y position to stay on owner's platform
        int targetY = (owner != null && owner.getMapId() == character.getMapId())
            ? owner.getPosition().y
            : current.y;

        // Only move horizontally (X axis) towards monster
        int dx = monsterPos.x - current.x;
        int direction = (dx > 0) ? 1 : -1;

        // Limit movement per tick (200ms * 125px/s = 25px per tick)
        int maxMove = (int) (WALK_SPEED * (MOVE_COOLDOWN / 1000.0));
        if (Math.abs(dx) > maxMove) dx = direction * maxMove;

        Point newPos = new Point(current.x + dx, targetY);

        // newstate: 1 = walk right, 2 = walk left
        int walkState = (direction > 0) ? 1 : 2;

        // Create movement packet (type 0 = absolute position + velocity)
        List<LifeMovementFragment> moves = new ArrayList<>();
        StaticLifeMovement move = new StaticLifeMovement(0, newPos, (int) MOVE_COOLDOWN, walkState, character.getFh());
        move.setPixelsPerSecond(new Point(direction * WALK_SPEED, 0));
        move.setFh((short) character.getFh());
        move.setUnk((short) 0);
        moves.add(move);

        // Broadcast movement to other players on the map
        character.getMap().broadcastMessage(character,
            MaplePacketCreator.movePlayer(character.getId(), moves, current), false);

        // Update stance and position on server (mirrors real PlayerHandler flow)
        handling.channel.handler.MovementParse.updatePosition(moves, character, 0);
        character.getMap().movePlayer(character, newPos);
    }

    // ==================== Combat AI ====================

    private boolean shouldAttack(long currentTime) {
        if (currentTime - lastAttackTime < ATTACK_COOLDOWN) return false;
        if (!character.isAlive()) return false;

        return findMonsterOnPlatform(attackRange) != null;
    }

    private void attackNearbyMonster() {
        MapleMonster monster = findMonsterOnPlatform(attackRange);
        if (monster == null) return;

        lastAttackTime = System.currentTimeMillis();
        currentState = State.ATTACKING;

        // Calculate damage (basic attack)
        int damage = calculateBasicAttackDamage();

        // Broadcast damage number before applying damage (monster may die in damage())
        byte[] damagePacket = MobPacket.damageMonster(monster.getObjectId(), damage);
        character.getMap().broadcastMessage(damagePacket, monster.getPosition());

        // Apply damage to monster
        monster.damage(character, damage, true);

        // Create attack pair for packet
        List<Pair<Integer, Boolean>> attackList = new ArrayList<>();
        attackList.add(new Pair<>(damage, false));
        AttackPair attackPair = new AttackPair(monster.getObjectId(), attackList);
        List<AttackPair> damageList = new ArrayList<>();
        damageList.add(attackPair);

        // tbyte encodes: (number of hits per target) | (number of targets << 4)
        // For 1 target with 1 hit: (1) | (1 << 4) = 1 | 16 = 17 = 0x11
        int tbyte = (1 << 4) | 1;

        // Attack animation parameters (values from real client attack packets):
        // - display: controls the attack direction/stance
        // - animation: the attack animation sequence (varies by weapon type)
        // - speed: attack speed (weapon attack speed)
        // Using values that real clients typically send for basic attacks
        byte display = (byte) 0;      // Neutral attack display
        byte animation = (byte) 0;    // Default animation
        byte speed = (byte) 6;        // Standard weapon speed

        // Broadcast attack animation to map
        // Parameters: cid, tbyte, skill, level, display, animation, speed, damage, energy, lvl, mastery, unk, charge
        byte[] packet = MaplePacketCreator.closeRangeAttack(character.getId(),
                tbyte, // tbyte - encoded hits and targets
                0, // skill (0 = basic attack)
                0, // level
                display, // display - attack stance/direction
                animation, // animation - attack animation frame
                speed, // speed - attack speed
                damageList,
                false, // energy
                character.getLevel(), // lvl
                (byte) 0, // mastery
                (byte) 0, // unk
                0); // charge

        // Broadcast attack animation to everyone on the map using position-based broadcast
        // This matches how real player attacks are broadcast
        character.getMap().broadcastMessage(packet, character.getPosition());

        // Reset state if monster died
        if (!monster.isAlive()) {
            currentState = State.IDLE;
        }
    }

    /**
     * Find closest monster on owner's platform within specified X range
     */
    private MapleMonster findMonsterOnPlatform(int xRange) {
        MapleMap map = character.getMap();
        if (map == null) return null;

        // Use owner's Y position as reference platform
        int platformY = (owner != null && owner.getMapId() == character.getMapId())
            ? owner.getPosition().y
            : character.getPosition().y;

        Point myPos = character.getPosition();
        MapleMonster closest = null;
        int closestXDist = Integer.MAX_VALUE;

        for (MapleMonster monster : map.getAllMonstersThreadsafe()) {
            if (monster == null || !monster.isAlive()) continue;

            Point mobPos = monster.getPosition();

            // Only consider monsters on owner's platform (similar Y position)
            int yDiff = Math.abs(mobPos.y - platformY);
            if (yDiff > SAME_PLATFORM_Y) continue;

            // Check X distance
            int xDist = Math.abs(mobPos.x - myPos.x);
            if (xDist <= xRange && xDist < closestXDist) {
                closestXDist = xDist;
                closest = monster;
            }
        }
        return closest;
    }

    private int calculateBasicAttackDamage() {
        // Simple damage calculation based on stats
        int atk = (int) character.getStat().getCurrentMaxBaseDamage();

        // Ensure minimum damage
        if (atk < 100) {
            atk = 100 + character.getLevel() * 10;
        }

        int minDmg = (int) (atk * 0.8);
        int maxDmg = atk;
        int damage = minDmg + (int) (Math.random() * (maxDmg - minDmg + 1));

        return Math.max(damage, 1); // At least 1 damage
    }

    // ==================== Chat AI ====================

    private static final int AI_TRIGGER_COUNT = 1; // 累積幾筆後呼叫 AI
    private final AtomicInteger chatCounter = new AtomicInteger(0);
    private final java.util.concurrent.atomic.AtomicBoolean aiPending = new java.util.concurrent.atomic.AtomicBoolean(false);

    /**
     * Called when someone on the same map sends a public chat message.
     * Counts messages; every AI_TRIGGER_COUNT messages calls the AI API and replies.
     *
     * @param history Shared per-map history buffer (最近 5 筆，包含 bot 自己)
     */
    public void onChat(MapleCharacter sender, String message, Deque<String> history) {
        if (!active) return;
        if (sender.getId() == character.getId()) return;
        if (VirtualPlayerManager.getInstance().isVirtualPlayer(sender.getId())) return;

        if (chatCounter.incrementAndGet() < AI_TRIGGER_COUNT) return;
        chatCounter.set(0);

        // 若 AI 仍在等待回覆，跳過此次觸發
        if (!aiPending.compareAndSet(false, true)) return;

        // 取當前 history snapshot
        String historySnapshot;
        synchronized (history) {
            historySnapshot = String.join("\n", history);
        }

        final String role = character.getName();
        final String snap = historySnapshot;

        // 非同步呼叫 AI，避免阻塞 scheduler thread
        server.Timer.MapTimer.getInstance().schedule(() -> {
            try {
                String reply = AiApiClient.chat(role, snap);
                if (reply != null && !reply.isEmpty()) {
                    say(reply);
                    VirtualPlayerManager.getInstance().appendBotChat(
                        character.getMapId(), role, reply);
                }
            } finally {
                aiPending.set(false);
            }
        }, 0);
    }

    /**
     * Broadcast a chat message from this bot to everyone on its map.
     */
    private void say(String text) {
        MapleMap map = character.getMap();
        if (map == null) return;

        byte[] packet = MaplePacketCreator.getChatText(character.getId(), text, false, 0);
        map.broadcastMessage(packet);
    }

    // ==================== Party AI ====================

    /**
     * Called when this bot receives a party invite
     */
    public void onPartyInvite(int partyId) {
        if (!active) return;

        MapleParty party = World.Party.getParty(partyId);
        if (party == null) return;

        // Auto-accept party invite
        if (party.getMembers().size() < 6) {
            World.Party.updateParty(partyId, PartyOperation.JOIN, new MaplePartyCharacter(character));
            character.receivePartyMemberHP();
            character.updatePartyMemberHP();
        }
    }

    /**
     * Check if this character is a virtual player
     */
    public static boolean isVirtualPlayer(MapleCharacter chr) {
        return VirtualPlayerManager.getInstance().isVirtualPlayer(chr.getId());
    }
}
