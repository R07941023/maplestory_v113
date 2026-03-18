package server.virtualplayer;

import client.MapleCharacter;
import client.MapleClient;
import handling.channel.ChannelServer;

/**
 * A wrapper for MapleClient used by virtual players (bots).
 * Uses composition since MapleClient methods are final.
 */
public class VirtualMapleClient {

    private final MapleClient client;
    private boolean connected = true;

    public VirtualMapleClient(int channel, int world) {
        // Create a real MapleClient with SafeMockIOSession
        this.client = new MapleClient(null, null, new SafeMockIOSession());
        this.client.setChannel(channel);
        this.client.setWorld(world);
    }

    public MapleClient getClient() {
        return client;
    }

    public void setPlayer(MapleCharacter player) {
        client.setPlayer(player);
    }

    public MapleCharacter getPlayer() {
        return client.getPlayer();
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public int getChannel() {
        return client.getChannel();
    }

    public int getWorld() {
        return client.getWorld();
    }

    public ChannelServer getChannelServer() {
        return ChannelServer.getInstance(client.getChannel());
    }

    public void disconnect() {
        connected = false;
        MapleCharacter player = client.getPlayer();
        if (player != null && player.getMap() != null) {
            player.getMap().removePlayer(player);
        }
    }
}
