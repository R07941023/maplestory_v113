package server.virtualplayer;

import client.MapleCharacter;
import client.MapleClient;
import database.DatabaseConnection;
import handling.channel.ChannelServer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Singleton manager for all virtual players (bots) in the server.
 */
public class VirtualPlayerManager {

    private static final VirtualPlayerManager instance = new VirtualPlayerManager();

    // Map of character ID to VirtualPlayer
    private final Map<Integer, VirtualPlayer> virtualPlayers = new ConcurrentHashMap<>();

    // Map of character name to character ID for quick lookup
    private final Map<String, Integer> nameToIdCache = new ConcurrentHashMap<>();

    // Per-map chat history buffer: mapId -> last 5 messages ("name: text")
    private static final int HISTORY_SIZE = 5;
    private final Map<Integer, Deque<String>> mapChatHistory = new ConcurrentHashMap<>();

    // Single shared AI timer for all bots
    private ScheduledFuture<?> globalAiTask = null;

    private VirtualPlayerManager() {
    }

    private void startGlobalAI() {
        if (globalAiTask != null) return;
        globalAiTask = server.Timer.MapTimer.getInstance().register(() -> {
            for (VirtualPlayer bot : virtualPlayers.values()) {
                try {
                    bot.tick();
                } catch (Exception e) {
                    System.err.println("[VirtualPlayerManager] AI tick error for "
                            + bot.getCharacter().getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }, 200);
    }

    private void stopGlobalAI() {
        if (globalAiTask != null) {
            globalAiTask.cancel(false);
            globalAiTask = null;
        }
    }

    public static VirtualPlayerManager getInstance() {
        return instance;
    }

    /**
     * Spawn a virtual player
     *
     * @param characterName The name of the character to spawn as bot
     * @param owner The GM who is spawning this bot
     * @return Result message
     */
    public String spawnBot(String characterName, MapleCharacter owner) {
        // Check if character exists
        int charId = getCharacterIdByName(characterName);
        if (charId == -1) {
            return "Failed: Character '" + characterName + "' not found.";
        }

        // Check if already a virtual player
        if (virtualPlayers.containsKey(charId)) {
            VirtualPlayer existing = virtualPlayers.get(charId);
            if (existing.getOwner() != null) {
                return "Failed: '" + characterName + "' is already controlled by " + existing.getOwner().getName() + ".";
            }
            return "Failed: '" + characterName + "' is already active as a bot.";
        }

        // Check if character is already logged in (real player)
        if (isCharacterOnline(charId)) {
            return "Failed: '" + characterName + "' is already logged in.";
        }

        // Load character from database
        int channel = owner.getClient().getChannel();
        int world = owner.getClient().getWorld();
        VirtualMapleClient virtualClient = new VirtualMapleClient(channel, world);

        MapleCharacter botChar = MapleCharacter.loadCharFromDB(charId, virtualClient.getClient(), true);
        if (botChar == null) {
            return "Failed: Could not load character '" + characterName + "' from database.";
        }

        virtualClient.setPlayer(botChar);

        // Create virtual player
        VirtualPlayer bot = new VirtualPlayer(botChar, owner, channel, world);

        // Register bot
        virtualPlayers.put(charId, bot);
        nameToIdCache.put(characterName.toLowerCase(), charId);

        // Activate bot on owner's map
        bot.activate(owner.getClient().getChannel(), owner.getMapId());

        // Start global AI timer (no-op if already running)
        startGlobalAI();

        // Register in channel server player storage
        ChannelServer cs = ChannelServer.getInstance(owner.getClient().getChannel());
        if (cs != null) {
            cs.getPlayerStorage().registerPlayer(botChar);
        }

        return "Bot '" + characterName + "' spawned and following you.";
    }

    /**
     * Release a virtual player (stop bot and log out)
     *
     * @param characterName The name of the bot to release
     * @param owner The GM who is releasing this bot
     * @return Result message
     */
    public String releaseBot(String characterName, MapleCharacter owner) {
        // Find bot by name
        Integer charId = nameToIdCache.get(characterName.toLowerCase());
        if (charId == null) {
            // Try direct lookup
            charId = getCharacterIdByName(characterName);
        }

        if (charId == null || charId == -1) {
            return "Failed: '" + characterName + "' is not found.";
        }

        VirtualPlayer bot = virtualPlayers.get(charId);
        if (bot == null) {
            return "Failed: '" + characterName + "' is not an active bot.";
        }

        // Check ownership
        if (bot.getOwner() != null && bot.getOwner().getId() != owner.getId()) {
            return "Failed: '" + characterName + "' is controlled by " + bot.getOwner().getName() + ", not you.";
        }

        // Deactivate and remove
        bot.deactivate();

        // Unregister from channel server
        MapleCharacter botChar = bot.getCharacter();
        if (botChar != null) {
            ChannelServer cs = ChannelServer.getInstance(owner.getClient().getChannel());
            if (cs != null) {
                cs.getPlayerStorage().deregisterPlayer(botChar);
            }
        }

        virtualPlayers.remove(charId);
        nameToIdCache.remove(characterName.toLowerCase());

        if (virtualPlayers.isEmpty()) {
            stopGlobalAI();
        }

        return "Bot '" + characterName + "' released and logged out.";
    }

    /**
     * List all active bots
     *
     * @return Formatted list of active bots
     */
    public String listBots() {
        if (virtualPlayers.isEmpty()) {
            return "No active bots.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Active bots (").append(virtualPlayers.size()).append("):\n");

        for (VirtualPlayer bot : virtualPlayers.values()) {
            MapleCharacter chr = bot.getCharacter();
            String ownerName = bot.getOwner() != null ? bot.getOwner().getName() : "None";
            sb.append("- ").append(chr.getName())
              .append(" (Lv.").append(chr.getLevel()).append(")")
              .append(" | Map: ").append(chr.getMapId())
              .append(" | Owner: ").append(ownerName)
              .append(" | State: ").append(bot.getCurrentState())
              .append("\n");
        }

        return sb.toString().trim();
    }

    /**
     * Check if a character ID is a virtual player
     */
    public boolean isVirtualPlayer(int characterId) {
        return virtualPlayers.containsKey(characterId);
    }

    /**
     * Get virtual player by character ID
     */
    public VirtualPlayer getVirtualPlayer(int characterId) {
        return virtualPlayers.get(characterId);
    }

    /**
     * Get virtual player by character name
     */
    public VirtualPlayer getVirtualPlayerByName(String name) {
        Integer charId = nameToIdCache.get(name.toLowerCase());
        if (charId == null) return null;
        return virtualPlayers.get(charId);
    }

    /**
     * Get all virtual players
     */
    public Collection<VirtualPlayer> getAllVirtualPlayers() {
        return virtualPlayers.values();
    }

    /**
     * Notify all bots on the same map that a player sent a public chat message.
     * Appends to per-map history buffer, then triggers each bot's AI when buffer hits 5.
     * Called by ChatHandler after the message is broadcast.
     */
    public void notifyMapChat(client.MapleCharacter sender, String message) {
        if (virtualPlayers.isEmpty()) return;

        int mapId = sender.getMapId();

        // Append to history buffer
        Deque<String> history = mapChatHistory.computeIfAbsent(mapId, k -> new ArrayDeque<>());
        synchronized (history) {
            history.addLast(sender.getName() + ": " + message);
            if (history.size() > HISTORY_SIZE) history.pollFirst();
        }

        // Notify bots on this map
        for (VirtualPlayer bot : virtualPlayers.values()) {
            if (bot.getCharacter().getMapId() == mapId) {
                bot.onChat(sender, message, history);
            }
        }
    }

    /**
     * Append a bot's own reply into the map history buffer.
     */
    void appendBotChat(int mapId, String botName, String message) {
        Deque<String> history = mapChatHistory.computeIfAbsent(mapId, k -> new ArrayDeque<>());
        synchronized (history) {
            history.addLast(botName + ": " + message);
            if (history.size() > HISTORY_SIZE) history.pollFirst();
        }
    }

    /**
     * Called when an owner (real player) changes map.
     * Immediately moves all bots owned by that player to the new map.
     */
    public void notifyOwnerMapChange(MapleCharacter owner) {
        if (virtualPlayers.isEmpty()) return;
        for (VirtualPlayer bot : virtualPlayers.values()) {
            if (bot.isActive() && bot.getOwner() != null && bot.getOwner().getId() == owner.getId()) {
                try {
                    bot.changeMapToOwner();
                } catch (Exception e) {
                    System.err.println("[VirtualPlayerManager] Error following owner map change for "
                            + bot.getCharacter().getName() + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Handle party invite for a virtual player
     */
    public void handlePartyInvite(int characterId, int partyId) {
        VirtualPlayer bot = virtualPlayers.get(characterId);
        if (bot != null && bot.isActive()) {
            bot.onPartyInvite(partyId);
        }
    }

    /**
     * Get character ID by name from database
     */
    private int getCharacterIdByName(String name) {
        Connection con = DatabaseConnection.getConnection();
        try (PreparedStatement ps = con.prepareStatement("SELECT id FROM characters WHERE name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            System.err.println("[VirtualPlayerManager] Error getting character ID: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Check if a character is currently online (real player)
     */
    private boolean isCharacterOnline(int characterId) {
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            MapleCharacter chr = cs.getPlayerStorage().getCharacterById(characterId);
            if (chr != null) {
                // Check if it's not already a virtual player
                if (!virtualPlayers.containsKey(characterId)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Called when a real player is about to log in.
     * Check if their character is a bot and handle conflict.
     */
    public boolean checkLoginConflict(int characterId) {
        return virtualPlayers.containsKey(characterId);
    }

    /**
     * Force release a bot by character ID (used when the real player logs in).
     */
    public void forceRelease(int characterId) {
        VirtualPlayer bot = virtualPlayers.get(characterId);
        if (bot == null) return;

        try {
            bot.deactivate();
        } catch (Exception e) {
            System.err.println("[VirtualPlayerManager] Error force-releasing bot " + characterId + ": " + e.getMessage());
        }

        MapleCharacter botChar = bot.getCharacter();
        if (botChar != null) {
            for (ChannelServer cs : ChannelServer.getAllInstances()) {
                cs.getPlayerStorage().deregisterPlayer(botChar);
            }
        }

        // Remove name cache entry
        if (botChar != null) {
            nameToIdCache.remove(botChar.getName().toLowerCase());
        }
        virtualPlayers.remove(characterId);

        if (virtualPlayers.isEmpty()) {
            stopGlobalAI();
        }

        System.out.println("[VirtualPlayerManager] Bot " + characterId + " force-released: real player logging in.");
    }

    /**
     * Shutdown all bots (called on server shutdown)
     */
    public void shutdown() {
        for (VirtualPlayer bot : virtualPlayers.values()) {
            try {
                bot.deactivate();
            } catch (Exception e) {
                System.err.println("[VirtualPlayerManager] Error deactivating bot: " + e.getMessage());
            }
        }
        virtualPlayers.clear();
        nameToIdCache.clear();
        stopGlobalAI();
    }
}
