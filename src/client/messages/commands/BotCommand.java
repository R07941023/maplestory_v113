package client.messages.commands;

import client.MapleClient;
import client.messages.CommandExecute;
import constants.ServerConstants;
import constants.ServerConstants.PlayerGMRank;
import server.virtualplayer.VirtualPlayerManager;

/**
 * Bot commands for managing virtual players.
 * Commands: !bot spawn <name>, !bot release <name>, !bot list
 */
public class BotCommand {

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.領導者; // GM level required
    }

    public static class Bot extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "Usage: !bot <spawn|release|list> [name]");
                return true;
            }

            String action = splitted[1].toLowerCase();
            VirtualPlayerManager manager = VirtualPlayerManager.getInstance();

            switch (action) {
                case "spawn": {
                    if (splitted.length < 3) {
                        c.getPlayer().dropMessage(6, "Usage: !bot spawn <characterName>");
                        return true;
                    }
                    String charName = splitted[2];
                    String result = manager.spawnBot(charName, c.getPlayer());
                    c.getPlayer().dropMessage(6, result);
                    return true;
                }

                case "release": {
                    if (splitted.length < 3) {
                        c.getPlayer().dropMessage(6, "Usage: !bot release <characterName>");
                        return true;
                    }
                    String charName = splitted[2];
                    String result = manager.releaseBot(charName, c.getPlayer());
                    c.getPlayer().dropMessage(6, result);
                    return true;
                }

                case "list": {
                    String result = manager.listBots();
                    // Split by newlines and send each as separate message
                    for (String line : result.split("\n")) {
                        c.getPlayer().dropMessage(6, line);
                    }
                    return true;
                }

                default:
                    c.getPlayer().dropMessage(6, "Unknown action. Use: spawn, release, list");
                    return true;
            }
        }

        @Override
        public String getMessage() {
            return new StringBuilder()
                .append(ServerConstants.PlayerGMRank.領導者.getCommandPrefix())
                .append("bot <spawn|release|list> [name] - Manage virtual players (bots)")
                .toString();
        }
    }

    public static class BotSpawn extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "Usage: !botspawn <characterName>");
                return true;
            }

            String charName = splitted[1];
            String result = VirtualPlayerManager.getInstance().spawnBot(charName, c.getPlayer());
            c.getPlayer().dropMessage(6, result);
            return true;
        }

        @Override
        public String getMessage() {
            return new StringBuilder()
                .append(ServerConstants.PlayerGMRank.領導者.getCommandPrefix())
                .append("botspawn <name> - Spawn a virtual player (bot)")
                .toString();
        }
    }

    public static class BotRelease extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "Usage: !botrelease <characterName>");
                return true;
            }

            String charName = splitted[1];
            String result = VirtualPlayerManager.getInstance().releaseBot(charName, c.getPlayer());
            c.getPlayer().dropMessage(6, result);
            return true;
        }

        @Override
        public String getMessage() {
            return new StringBuilder()
                .append(ServerConstants.PlayerGMRank.領導者.getCommandPrefix())
                .append("botrelease <name> - Release a virtual player (bot)")
                .toString();
        }
    }

    public static class BotList extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            String result = VirtualPlayerManager.getInstance().listBots();
            for (String line : result.split("\n")) {
                c.getPlayer().dropMessage(6, line);
            }
            return true;
        }

        @Override
        public String getMessage() {
            return new StringBuilder()
                .append(ServerConstants.PlayerGMRank.領導者.getCommandPrefix())
                .append("botlist - List all active bots")
                .toString();
        }
    }
}
