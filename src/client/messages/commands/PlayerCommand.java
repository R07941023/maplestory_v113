package client.messages.commands;

import client.MapleCharacter;
import client.messages.CommandExecute;
import constants.GameConstants;
import client.MapleClient;
import client.MapleStat;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import constants.PiPiConfig;
import constants.ServerConfig;
import constants.ServerConstants;
import constants.ServerConstants.PlayerGMRank;
import constants.WorldConstants;
import scripting.NPCScriptManager;
import tools.MaplePacketCreator;
import server.life.MapleMonster;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import java.util.Arrays;
import tools.StringUtil;
import handling.world.World;
import java.awt.Point;
import java.util.Calendar;
import scripting.ReactorScriptManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.Randomizer;
import server.Timer;
import server.life.MapleLifeFactory;
import server.life.MapleMonsterInformationProvider;
import server.life.OverrideMonsterStats;
import server.maps.MapleMap;
import server.swing.WvsCenter;
import tools.FilePrinter;
import tools.FileoutputUtil;

/**
 *
 * @author Emilyx3
 */
public class PlayerCommand {

    public static PlayerGMRank getPlayerLevelRequired() {
        return ServerConstants.PlayerGMRank.普通玩家;
    }

    public static class help extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropNPC(""
                    + "\t   #i3994014##i3994018##i3994070##i3994061##i3994005##i3991038##i3991004#\r\n"
                    + "\t\t  #fMob/0100101.img/move/1##b 親愛的： #h \r\n"
                    + " #fMob/0100101.img/move/1##k\r\r\n"
                    + "\t\t#fMob/0130101.img/move/1##g[以下是" + c.getChannelServer().getServerName() + " 玩家指令]#k#fMob/0130101.img/move/1#\r\n"
                    + "\t  #r▇▇▆▅▄▃▂#d萬用指令區#r▂▃▄▅▆▇▇\r\n"
                    + "\t\t#b@清除道具 <裝備欄/消耗欄/裝飾欄/其他欄/特殊欄> <開始格數> <結束格數>#k - #r<清除背包道具>#k\r\n"
                    + "\t\t#b@ea#k - #r<解除異常+查看當前狀態>#k\r\n"
                    + "\t\t#b@在線點數/@jcds#k - #r<領取在線點數>#k\r\n"
                    + "\t\t#b@mob#k - #r<查看身邊怪物訊息>#k\r\n"
                    + "\t\t#b@expfix#k - #r<經驗歸零(修復假死)>#k\r\n"
                    + "\t\t#b@CGM <訊息>#k - #r<傳送訊息給GM>#k\r\n"
                    + "\t\t#b@jk_hm #k - #r<清除卡精靈商人>#k\r\n"
                    + "\t\t#b@save#k - #r<存檔>#k\r\n"
                    + "\t\t#b@TSmega#k - #r<開/關所有廣播>#k\r\n"       
                    + "\t\t#b@dice#k - #r<單骰>#k\r\n"
                    + "\t\t#b@dice3#k - #r<三骰>#k\r\n"              
                    + "\t\t#b@轉蛋綠廣#k - #r<開關轉蛋綠廣>#k\r\n"                              
                    + "\t\t#b@str @dex @int @luk 數量#k - #r<能力分配>#k\r\n"   
                    + "\t\t#b@sell <裝備欄/消耗欄/裝飾欄/其他欄/特殊欄> <開始格數> <結束格數>#k - #r<販賣背包道具>#k\r\n"                         
            );
            return true;
        }

        @Override
        public String getMessage() {
            return new StringBuilder().append(ServerConstants.PlayerGMRank.普通玩家.getCommandPrefix()).append("help - 幫助").toString();
        }
    }

    public abstract static class OpenNPCCommand extends CommandExecute {

        protected int npc = -1;
        private static final int[] npcs = { //Ish yur job to make sure these are in order and correct ;(
            9010017};

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (npc != 1 && c.getPlayer().getMapId() != 910000000) { //drpcash can use anywhere
                for (int i : GameConstants.blockedMaps) {
                    if (c.getPlayer().getMapId() == i) {
                        c.getPlayer().dropMessage(1, "你不能在這裡使用指令.");
                        return true;
                    }
                }
                if (c.getPlayer().getLevel() < 10) {
                    c.getPlayer().dropMessage(1, "你的等級必須是10等.");
                    return true;
                }
                if (c.getPlayer().getMap().getSquadByMap() != null || c.getPlayer().getEventInstance() != null || c.getPlayer().getMap().getEMByMap() != null || c.getPlayer().getMapId() >= 990000000/* || FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit())*/) {
                    c.getPlayer().dropMessage(1, "你不能在這裡使用指令.");
                    return true;
                }
                if ((c.getPlayer().getMapId() >= 680000210 && c.getPlayer().getMapId() <= 680000502) || (c.getPlayer().getMapId() / 1000 == 980000 && c.getPlayer().getMapId() != 980000000) || (c.getPlayer().getMapId() / 100 == 1030008) || (c.getPlayer().getMapId() / 100 == 922010) || (c.getPlayer().getMapId() / 10 == 13003000)) {
                    c.getPlayer().dropMessage(1, "你不能在這裡使用指令.");
                    return true;
                }
            }
            NPCScriptManager.getInstance().start(c, npcs[npc]);
            return true;
        }
    }

    public static class save extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            try {
                int res = c.getPlayer().saveToDB(true, true);
                if (res == 1) {
                    c.getPlayer().dropMessage(5, "保存成功！");
                } else {
                    c.getPlayer().dropMessage(5, "保存失敗！");
                }
            } catch (UnsupportedOperationException ex) {

            }
            return true;
        }

        @Override
        public String getMessage() {
            return new StringBuilder().append(ServerConstants.PlayerGMRank.普通玩家.getCommandPrefix()).append("save - 存檔").toString();
        }
    }

    public static class expfix extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            c.getPlayer().setExp(0);
            c.getPlayer().updateSingleStat(MapleStat.EXP, c.getPlayer().getExp());
            c.getPlayer().dropMessage(5, "經驗修復完成");
            return true;
        }

        @Override
        public String getMessage() {
            return new StringBuilder().append(ServerConstants.PlayerGMRank.普通玩家.getCommandPrefix()).append("expfix - 經驗歸零").toString();
        }
    }

     public static class STR extends DistributeStatCommands {

        public STR() {
            stat = MapleStat.STR;
        }
    }

    public static class DEX extends DistributeStatCommands {

        public DEX() {
            stat = MapleStat.DEX;
        }
    }

    public static class INT extends DistributeStatCommands {

        public INT() {
            stat = MapleStat.INT;
        }
    }

    public static class LUK extends DistributeStatCommands {

        public LUK() {
            stat = MapleStat.LUK;
        }
    }

    public abstract static class DistributeStatCommands extends CommandExecute {

        protected MapleStat stat = null;
        private static int statLim = 32767;

        private void setStat(MapleCharacter player, int amount) {
            switch (stat) {
                case STR:
                    player.getStat().setStr((short) amount, player);
                    player.updateSingleStat(MapleStat.STR, player.getStat().getStr());
                    break;
                case DEX:
                    player.getStat().setDex((short) amount, player);
                    player.updateSingleStat(MapleStat.DEX, player.getStat().getDex());
                    break;
                case INT:
                    player.getStat().setInt((short) amount, player);
                    player.updateSingleStat(MapleStat.INT, player.getStat().getInt());
                    break;
                case LUK:
                    player.getStat().setLuk((short) amount, player);
                    player.updateSingleStat(MapleStat.LUK, player.getStat().getLuk());
                    break;
            }
        }

        private int getStat(MapleCharacter player) {
            switch (stat) {
                case STR:
                    return player.getStat().getStr();
                case DEX:
                    return player.getStat().getDex();
                case INT:
                    return player.getStat().getInt();
                case LUK:
                    return player.getStat().getLuk();
                default:
                    throw new RuntimeException(); //Will never happen.
            }
        }

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "Invalid number entered.");
                return false;
            }
            int change = 0;
            try {
                change = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException nfe) {
                c.getPlayer().dropMessage(5, "輸入的數字無效.");
                return false;
            }
            if (change <= 0) {
                c.getPlayer().dropMessage(5, "您必須輸入一個大於 0 的數字.");
                return false;
            }
            if (c.getPlayer().getRemainingAp() < change) {
                c.getPlayer().dropMessage(5, "您的能力點不足.");
                return false;
            }
            if (getStat(c.getPlayer()) + change > statLim) {
                c.getPlayer().dropMessage(5, "所要分配的能力點總和不能大於 " + statLim + "點.");
                return false;
            }
            setStat(c.getPlayer(), getStat(c.getPlayer()) + change);
            c.getPlayer().setRemainingAp((short) (c.getPlayer().getRemainingAp() - change));
            c.getPlayer().updateSingleStat(MapleStat.AVAILABLEAP, c.getPlayer().getRemainingAp());
            c.getPlayer().dropMessage(5, StringUtil.makeEnumHumanReadable(stat.name()) + "提高了 " + change + "點.");
            return true;
        }


        @Override
        public String getMessage() {
            return new StringBuilder().append(ServerConstants.PlayerGMRank.普通玩家.getCommandPrefix()).append("@str @dex @int @luk 數量 - 能力分配" ).toString();
        }        
    }
    
    public static class 寵吸 extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            c.getPlayer().setHasVac();
            return true;
        }

        @Override
        public String getMessage() {
            return new StringBuilder().append("@寵吸 - 開/關閉寵吸").toString();
        }
    }

    public static class TSmega extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            c.getPlayer().setSmega();
            return true;
        }

        @Override
        public String getMessage() {
            return new StringBuilder().append(ServerConstants.PlayerGMRank.普通玩家.getCommandPrefix()).append("TSmega - 開/關閉廣播").toString();
        }
    }

    public static class ea extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            c.removeClickedNPC();
            NPCScriptManager.getInstance().dispose(c);
            c.sendPacket(MaplePacketCreator.enableActions());
            int 人物經驗=1;
            if (c.getPlayer().getLevel() < 10) {
                    人物經驗 = WorldConstants.EXP_RATE;
                } else if (c.getPlayer().getLevel() < 120) {
                    人物經驗 =ServerConfig.levelexp1;
                } else if (c.getPlayer().getLevel() < 200) {
                    人物經驗 =ServerConfig.levelexp2;
                } else if (c.getPlayer().getLevel() < 250) {
                    人物經驗 =ServerConfig.levelexp3;
                }else {
                    人物經驗 =WorldConstants.EXP_RATE;
                }           
            
            c.getPlayer().dropMessage(1, "解卡完畢..");
            c.getPlayer().dropMessage(6,"＊＊＊＊＊＊＊歡迎來到台積谷＊＊＊＊＊＊＊");  
            c.getPlayer().dropMessage(6, "當前系統時間" + FilePrinter.getLocalDateString() + " 星期" + getDayOfWeek());
            c.getPlayer().dropMessage(6, "目前等級倍率" + (Math.round(人物經驗)) + "倍");            
            c.getPlayer().dropMessage(6, "經驗值倍率 " + ((Math.round(c.getPlayer().getEXPMod()) * 100) * Math.round(c.getPlayer().getStat().expBuff / 100.0) + (c.getPlayer().getStat().equippedFairy ? c.getPlayer().getFairyExp() : 0)) + "%, 掉寶倍率 " + Math.round(c.getPlayer().getDropMod() * (c.getPlayer().getStat().dropBuff / 100.0) * 100) + "%, 楓幣倍率 " + Math.round((c.getPlayer().getStat().mesoBuff / 100.0) * 100) + "% VIP經驗加成：" + c.getPlayer().getVipExpRate() + "%");
            if (c.getChannelServer().getExExpRate() > 1 || c.getChannelServer().getExDropRate() > 1 || c.getChannelServer().getExMesoRate() > 1) {
                c.getPlayer().dropMessage(6, "額外經驗值倍率 " + (c.getChannelServer().getExExpRate()) + "倍, 掉寶倍率 " + (c.getChannelServer().getExDropRate()) + "倍, 楓幣倍率 " + (c.getChannelServer().getExMesoRate()) + "倍");
            }
            c.getPlayer().dropMessage(6, "目前剩餘 " + c.getPlayer().getCSPoints(1) + " GASH " + c.getPlayer().getCSPoints(2) + " 楓葉點數 ");
            c.getPlayer().dropMessage(6, "當前延遲 " + c.getPlayer().getClient().getLatency() + " 毫秒");
            c.getPlayer().dropMessage(6, "已使用:" + c.getPlayer().getHpMpApUsed() + " 張能力重置捲");
            if (c.getPlayer().getLevel() >= 120 && c.getPlayer().getQuestStatus(29400) == 1) {
                c.getPlayer().dropMessage(6, "精明的獵人已經擊殺:" + c.getPlayer().getMobCount() + "隻怪物.");
            }
            return true;
        }

        @Override
        public String getMessage() {
            return new StringBuilder().append(ServerConstants.PlayerGMRank.普通玩家.getCommandPrefix()).append("ea - 解卡").toString();
        }

        public static String getDayOfWeek() {
            int dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;
            String dd = String.valueOf(dayOfWeek);
            switch (dayOfWeek) {
                case 0:
                    dd = "日";
                    break;
                case 1:
                    dd = "一";
                    break;
                case 2:
                    dd = "二";
                    break;
                case 3:
                    dd = "三";
                    break;
                case 4:
                    dd = "四";
                    break;
                case 5:
                    dd = "五";
                    break;
                case 6:
                    dd = "六";
                    break;
            }
            return dd;
        }
    }

//    public static class JK extends CommandExecute {
//
//        @Override
//        public boolean execute(MapleClient c, String[] splitted) {
//            for (int i : GameConstants.blockedMaps) {
//                if (c.getPlayer().getMapId() == i) {
//                    c.getPlayer().dropMessage(1, "你不能在這裡使用指令.");
//                    return true;
//                }
//            }
//            if (c.getPlayer().getLevel() < 10) {
//                c.getPlayer().dropMessage(1, "你的等級必須是10等.");
//                return true;
//            }
//            if (c.getPlayer().getMap().getSquadByMap() != null || c.getPlayer().getEventInstance() != null || c.getPlayer().getMap().getEMByMap() != null || c.getPlayer().getMapId() >= 990000000/* || FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit())*/) {
//                c.getPlayer().dropMessage(1, "你不能在這裡使用指令.");
//                return true;
//            }
//            if ((c.getPlayer().getMapId() >= 680000210 && c.getPlayer().getMapId() <= 680000502) || (c.getPlayer().getMapId() / 1000 == 980000 && c.getPlayer().getMapId() != 980000000) || (c.getPlayer().getMapId() / 100 == 1030008) || (c.getPlayer().getMapId() / 100 == 922010) || (c.getPlayer().getMapId() / 10 == 13003000)) {
//                c.getPlayer().dropMessage(1, "你不能在這裡使用指令.");
//                return true;
//            }
//            InterServerHandler.EnterCashShop(c, c.getPlayer(), false);
//            return true;
//        }
//
//        @Override
//        public String getMessage() {
//            return new StringBuilder().append(ServerConstants.PlayerGMRank.普通玩家.getCommandPrefix()).append("jk - 重製").toString();
//        }
//    }
    public static class mob extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            MapleMonster monster = null;
            for (final MapleMapObject monstermo : c.getPlayer().getMap().getMapObjectsInRange(c.getPlayer().getPosition(), 100000, Arrays.asList(MapleMapObjectType.MONSTER))) {
                monster = (MapleMonster) monstermo;
                if (monster.isAlive()) {
                    c.getPlayer().dropMessage(6, "怪物 " + monster.toString());
                }
            }
            if (monster == null) {
                c.getPlayer().dropMessage(6, "找不到地圖上的怪物");
            }
            return true;
        }

        @Override
        public String getMessage() {
            return new StringBuilder().append(ServerConstants.PlayerGMRank.普通玩家.getCommandPrefix()).append("mob - 查看怪物狀態").toString();
        }
    }

    public static class CGM extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            boolean autoReply = false;

            if (splitted.length < 2) {
                return false;
            }
            String talk = StringUtil.joinStringFrom(splitted, 1);
            if (c.getPlayer().isGM()) {
                c.getPlayer().dropMessage(6, "因為你自己是GM所以無法使用此指令,可以嘗試!cngm <訊息> 來建立GM聊天頻道~");
            } else {
                if (!c.getPlayer().getCheatTracker().GMSpam(100000, 1)) { // 1 minutes.
                    boolean fake = false;
                    boolean showmsg = true;

                    // 管理員收不到，玩家有顯示傳送成功
                    if (PiPiConfig.getBlackList().containsKey(c.getAccID())) {
                        fake = true;
                    }

                    // 管理員收不到，玩家沒顯示傳送成功
                    if (talk.contains("搶") && talk.contains("圖")) {
                        c.getPlayer().dropMessage(1, "搶圖自行解決！！");
                        fake = true;
                        showmsg = false;
                    } else if ((talk.contains("被") && talk.contains("騙")) || (talk.contains("點") && talk.contains("騙"))) {
                        c.getPlayer().dropMessage(1, "被騙請自行解決");
                        fake = true;
                        showmsg = false;
                    } else if ((talk.contains("被") && talk.contains("盜"))) {
                        c.getPlayer().dropMessage(1, "被盜請自行解決");
                        fake = true;
                        showmsg = false;
                    } else if (talk.contains("刪") && ((talk.contains("角") || talk.contains("腳")) && talk.contains("錯"))) {
                        c.getPlayer().dropMessage(1, "刪錯角色請自行解決");
                        fake = true;
                        showmsg = false;
                    } else if (talk.contains("亂") && (talk.contains("名") && talk.contains("聲"))) {
                        c.getPlayer().dropMessage(1, "請自行解決");
                        fake = true;
                        showmsg = false;
                    } else if (talk.contains("密") && talk.contains("咒") && talk.contains("賣")) {
                        c.getPlayer().dropMessage(1, "密咒賣的價格已經更改為1楓幣無誤");
                        fake = true;
                        showmsg = false;
                    } else if (talk.contains("改") && talk.contains("密") && talk.contains("碼")) {
                        c.getPlayer().dropMessage(1, "目前第二組密碼及密碼無法查詢及更改,");
                        fake = true;
                        showmsg = false;
                    }

                    // 管理員收的到，自動回復
                    if (talk.toUpperCase().contains("VIP") && ((talk.contains("領") || (talk.contains("獲"))) && talk.contains("取"))) {
                        c.getPlayer().dropMessage(1, "VIP將會於儲值後一段時間後自行發放，請耐心等待");
                        autoReply = true;
                    } else if (talk.contains("貢獻") || talk.contains("666") || ((talk.contains("取") || talk.contains("拿") || talk.contains("發") || talk.contains("領")) && ((talk.contains("勳") || talk.contains("徽") || talk.contains("勛")) && talk.contains("章")))) {
                        c.getPlayer().dropMessage(1, "勳章請去點拍賣NPC案領取勳章\r\n如尚未被加入清單請耐心等候GM。");
                        autoReply = true;
                    } else if (((talk.contains("商人") || talk.contains("精靈")) && talk.contains("吃")) || (talk.contains("商店") && talk.contains("補償"))) {
                        c.getPlayer().dropMessage(1, "目前精靈商人裝備和楓幣有機率被吃\r\n如被吃了請務必將當時的情況完整描述給管理員\r\n\r\nPS: 不會補償任何物品");
                        autoReply = true;
                    } else if (talk.contains("檔") && talk.contains("案") && talk.contains("受") && talk.contains("損")) {
                        c.getPlayer().dropMessage(1, "檔案受損請重新解壓縮主程式唷");
                        autoReply = true;
                    } else if ((talk.contains("缺") || talk.contains("少")) && ((talk.contains("技") && talk.contains("能") && talk.contains("點")) || talk.toUpperCase().contains("SP"))) {
                        c.getPlayer().dropMessage(1, "缺少技能點請重練，沒有其他方法了唷");
                        autoReply = true;

                    } else if (talk.contains("母書")) {
                        if (talk.contains("火流星")) {
                            c.getPlayer().dropMessage(1, "技能[火流星] 並沒有母書唷");
                            autoReply = true;
                        }
                    } else if (talk.contains("黑符") && talk.contains("不") && (talk.contains("掉") || talk.contains("噴"))) {
                        MapleMonsterInformationProvider.getInstance().clearDrops();
                        ReactorScriptManager.getInstance().clearDrops();
                        c.getPlayer().dropMessage(1, "黑符掉落機率偏低\r\n請打150場以上沒有噴再回報");
                        autoReply = true;
                    } else if (talk.contains("鎖") && talk.contains("寶")) {
                        c.getPlayer().dropMessage(1, "本伺服器目前並未鎖寶\r\n只有尚未添加的掉寶資料或是掉落機率偏低");
                        autoReply = true;
                    }

                    if (showmsg) {
                        c.sendCGMLog(c, talk);
                        c.getPlayer().dropMessage(6, "訊息已經寄送給GM了!");
                    }

                    if (!fake) {
                        World.Broadcast.broadcastGMMessage(MaplePacketCreator.getItemNotice("[管理員幫幫忙]頻道 " + c.getPlayer().getClient().getChannel() + " 玩家 [" + c.getPlayer().getName() + "] (" + c.getPlayer().getId() + "): " + talk + (autoReply ? " -- (系統已自動回復)" : "")));
                        if (System.getProperty("StartBySwing") != null) {
                            WvsCenter.addChatLog("[管理員幫幫忙] " + c.getPlayer().getName() + ": " + StringUtil.joinStringFrom(splitted, 1) + (autoReply ? " -- (系統已自動回復)" : "") + "\r\n");
                        }
                    }

                    FileoutputUtil.logToFile("logs/data/管理員幫幫忙.txt", "\r\n " + FileoutputUtil.NowTime() + " 玩家[" + c.getPlayer().getName() + "] 帳號[" + c.getAccountName() + "]: " + talk + (autoReply ? " -- (系統已自動回復)" : "") + "\r\n");

                } else {
                    c.getPlayer().dropMessage(6, "為了防止對GM刷屏所以每1分鐘只能發一次.");
                }
            }
            return true;
        }

        @Override
        public String getMessage() {
            return new StringBuilder().append(ServerConstants.PlayerGMRank.普通玩家.getCommandPrefix()).append("cgm - 跟GM回報").toString();
        }
    }

    public static class Sell extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 4) {
                return false;
            }
            MapleInventory inv;
            MapleInventoryType type;
            String Column = "null";
            int start = -1;
            int end = -1;
            try {
                Column = splitted[1];
                start = Integer.parseInt(splitted[2]);
                end = Integer.parseInt(splitted[3]);
            } catch (Exception ex) {
            }
            if (start == -1 || end == -1) {
                c.getPlayer().dropMessage("@sell  <裝備欄/消耗欄/裝飾欄/其他欄/特殊欄> <開始格數> <結束格數>");
                return true;
            }
            if (start < 1) {
                start = 1;
            }
            if (end > 96) {
                end = 96;
            }

            switch (Column) {
                case "裝備欄":
                    type = MapleInventoryType.EQUIP;
                    break;
                case "消耗欄":
                    type = MapleInventoryType.USE;
                    break;
                case "裝飾欄":
                    type = MapleInventoryType.SETUP;
                    break;
                case "其他欄":
                    type = MapleInventoryType.ETC;
                    break;
                case "特殊欄":
                    type = MapleInventoryType.CASH;
                    break;
                default:
                    type = null;
                    break;
            }
            if (type == null) {
                c.getPlayer().dropMessage("@sell  <裝備欄/消耗欄/裝飾欄/其他欄/特殊欄> <開始格數> <結束格數>");
                return true;
            }
            inv = c.getPlayer().getInventory(type);
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            int totalMesosGained = 0;            

            for (int i = start; i <= end; i++) {
                if (inv.getItem((short) i) != null) {
                        int itemPrice = (int) ii.getPrice(inv.getItem((short) i).getItemId());
                        totalMesosGained += itemPrice;                       
                    if (!ii.cantSell(inv.getItem((short) i).getItemId())) {
                        if (!GameConstants.isRechargable(inv.getItem((short) i).getItemId())) {
                            if (inv.getItem((short) i).getQuantity() > 0) {
                                double price;
                                if (GameConstants.isThrowingStar(inv.getItem((short) i).getItemId()) || GameConstants.isBullet(inv.getItem((short) i).getItemId())) {
                                    price = ii.getWholePrice(inv.getItem((short) i).getItemId()) / (double) ii.getSlotMax(c, inv.getItem((short) i).getItemId());
                                } else {
                                    price = ii.getPrice(inv.getItem((short) i).getItemId());
                                }
                                if (inv.getItem((short) i).getItemId() == 2022195) {
                                    price = 1;
                                }
                                if (inv.getItem((short) i).getItemId() == 4031348) {
                                    price = 1;
                                }

                                int quantity = inv.getItem((short) i).getQuantity();
                                int itemId = inv.getItem((short) i).getItemId();
                                MapleInventoryManipulator.removeFromSlot(c, type, (short) i, inv.getItem((short) i).getQuantity(), true);

                                int recvMesos = (int) Math.max(Math.ceil(price * quantity), 0);
                                if (price != -1.0 && recvMesos > 0) {
                                    if (recvMesos > PiPiConfig.商店一次拍賣獲得最大楓幣) {
                                        recvMesos = 1;
                                    }
                                    c.getPlayer().gainMeso(recvMesos, false);
                                }
                                FileoutputUtil.logToFile("logs/Data/指令販售道具.txt", "\r\n " + FileoutputUtil.NowTime() + " IP: " + c.getSession().remoteAddress().toString().split(":")[0] + " 帳號: " + c.getAccountName() + " 玩家: " + c.getPlayer().getName() + " 販售道具 " + ii.getName(itemId) + "x" + quantity + " " + recvMesos + " 楓幣");
                            }
                        }
                    }

                }
            }
            FileoutputUtil.logToFile("logs/Data/玩家指令.txt", "\r\n " + FileoutputUtil.NowTime() + " IP: " + c.getSession().remoteAddress().toString().split(":")[0] + " 帳號: " + c.getAccountName() + " 玩家: " + c.getPlayer().getName() + " 使用了指令 " + StringUtil.joinStringFrom(splitted, 0));

            c.getPlayer().dropMessage(6, "您已經販賣了第 " + start + " 格到 " + end + "格得到" + totalMesosGained + "元");
            return true;
        }

        @Override
        public String getMessage() {
            return new StringBuilder().append("@sell <裝備欄/消耗欄/裝飾欄/其他欄/特殊欄> <開始格數> <結束格數>").toString();
        }
    }
    
     public static class charinfo extends CommandExecute {
        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            StringBuilder sb = new StringBuilder();
            sb.append("-----------以下是角色屬性(不含技能加層的)-----------");
            sb.append("\r\n力量:").append(c.getPlayer().getStat().getStr()).append("\t\t總力量:").append(c.getPlayer().getStat().getTotalStr());
            sb.append("\r\n敏捷:").append(c.getPlayer().getStat().getDex()).append("\t\t總敏捷:").append(c.getPlayer().getStat().getTotalDex());
            sb.append("\r\n智力:").append(c.getPlayer().getStat().getInt()).append("\t\t總智力:").append(c.getPlayer().getStat().getTotalInt());
            sb.append("\r\n幸運:").append(c.getPlayer().getStat().getLuk()).append("\t\t總幸運:").append(c.getPlayer().getStat().getTotalLuk());
            sb.append("\r\n-----------以下是個人角色資訊-----------");
            sb.append("\r\n血量:").append(c.getPlayer().getStat().getHp()).append("/").append(c.getPlayer().getStat().getCurrentMaxHp());
            sb.append("\r\n魔量:").append(c.getPlayer().getStat().getMp()).append("/").append(c.getPlayer().getStat().getCurrentMaxMp());
            sb.append("\r\n經驗值:").append(c.getPlayer().getExp());
            sb.append("\r\n爆擊率").append(c.getPlayer().getStat().passive_sharpeye_rate()).append("%");
            sb.append("\r\n物理攻擊力:").append(c.getPlayer().getStat().getTotalWatk());
            sb.append("\r\n魔法攻擊力:").append(c.getPlayer().getStat().getTotalMagic());
            sb.append("\r\n最高攻擊:").append(c.getPlayer().getStat().getCurrentMaxBaseDamage());
            sb.append("\r\n總傷害:").append((int) Math.ceil(c.getPlayer().getStat().dam_r - 100)).append("%");
            sb.append("\r\nBOSS攻擊力:").append((int) Math.ceil(c.getPlayer().getStat().bossdam_r - 100)).append("%");
            c.getPlayer().dropNPC(sb.toString());
            return true;
        }

        @Override
        public String getMessage() {
            return new StringBuilder().append(PlayerGMRank.普通玩家.getCommandPrefix()).append("charinfo - 查看本身自己的角色訊息").toString();
        }
    }
 
    public static class 清除道具 extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 4) {
                return false;
            }
            MapleInventory inv;
            MapleInventoryType type;
            String Column = "null";
            int start = -1;
            int end = -1;
            try {
                Column = splitted[1];
                start = Integer.parseInt(splitted[2]);
                end = Integer.parseInt(splitted[3]);
            } catch (Exception ex) {
            }
            if (start == -1 || end == -1) {
                c.getPlayer().dropMessage("@清除道具 <裝備欄/消耗欄/裝飾欄/其他欄/特殊欄> <開始格數> <結束格數>");
                return true;
            }
            if (start < 1) {
                start = 1;
            }
            if (end > 96) {
                end = 96;
            }

            switch (Column) {
                case "裝備欄":
                    type = MapleInventoryType.EQUIP;
                    break;
                case "消耗欄":
                    type = MapleInventoryType.USE;
                    break;
                case "裝飾欄":
                    type = MapleInventoryType.SETUP;
                    break;
                case "其他欄":
                    type = MapleInventoryType.ETC;
                    break;
                case "特殊欄":
                    type = MapleInventoryType.CASH;
                    break;
                default:
                    type = null;
                    break;
            }
            if (type == null) {
                c.getPlayer().dropMessage("@清除道具 <裝備欄/消耗欄/裝飾欄/其他欄/特殊欄> <開始格數> <結束格數>");
                return true;
            }
            inv = c.getPlayer().getInventory(type);

            for (int i = start; i <= end; i++) {
                if (inv.getItem((short) i) != null) {
                    MapleInventoryManipulator.removeFromSlot(c, type, (short) i, inv.getItem((short) i).getQuantity(), true);
                }
            }
            FileoutputUtil.logToFile("logs/Data/玩家指令.txt", "\r\n " + FileoutputUtil.NowTime() + " IP: " + c.getSession().remoteAddress().toString().split(":")[0] + " 帳號: " + c.getAccountName() + " 玩家: " + c.getPlayer().getName() + " 使用了指令 " + StringUtil.joinStringFrom(splitted, 0));
            c.getPlayer().dropMessage(6, "您已經清除了第 " + start + " 格到 " + end + "格的" + Column + "道具");
            return true;
        }

        @Override
        public String getMessage() {
            return new StringBuilder().append(ServerConstants.PlayerGMRank.普通玩家.getCommandPrefix()).append("清除道具 <裝備欄/消耗欄/裝飾欄/其他欄/特殊欄> <開始格數> <結束格數>").toString();
        }
    }


    
    public static class jk_hm extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            c.getPlayer().RemoveHired();
            c.getPlayer().dropMessage("卡精靈商人已經解除");
            return true;
        }

        @Override
        public String getMessage() {
            return new StringBuilder().append(ServerConstants.PlayerGMRank.普通玩家.getCommandPrefix()).append("jk_hm - 卡精靈商人解除").toString();
        }
    }

  /*   public static class ItemVac extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String splitted[]) {
            boolean ItemVac = c.getPlayer().getItemVac();
            if (ItemVac == false) {
                c.getPlayer().stopItemVac();
                c.getPlayer().startItemVac();
            } else {
                c.getPlayer().stopItemVac();
            }
            c.getPlayer().dropMessage(6, "目前自動撿物狀態:" + (ItemVac == false ? "開啟" : "關閉"));
            return true;

        }

        @Override
        public String getMessage() {
           return new StringBuilder().append(ServerConstants.PlayerGMRank.普通玩家.getCommandPrefix()).append("ItemVac- 開啟玩家全圖吸").toString();
        }
     }*/
   
    public static class reborn extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            int result = c.getPlayer().doReborn(0);
            if (result == -1) {
                c.getPlayer().dropMessage("200等後才能轉生");
            } else if (result == 1) {
                c.getPlayer().dropMessage("轉生成功");
            }
            return true;
        }

        @Override
        public String getMessage() {
           return new StringBuilder().append(ServerConstants.PlayerGMRank.普通玩家.getCommandPrefix()).append("reborn- 轉生").toString();
        }
    }
   

     public static class jcds extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            int gain = c.getPlayer().getMP();
            if (gain <= 0) {
                c.getPlayer().dropMessage("目前沒有任何在線點數唷。");
                return true;
            }
            if (splitted.length < 2) {
                c.getPlayer().dropMessage("目前楓葉點數: " + c.getPlayer().getCSPoints(2));
                c.getPlayer().dropMessage("目前在線點數已經累積: " + gain + " 點，若要領取請輸入 @jcds true");
            } else if ("true".equals(splitted[1])) {
                gain = c.getPlayer().getMP();
                c.getPlayer().modifyCSPoints(2, gain, true);
                c.getPlayer().setMP(0);
                c.getPlayer().saveToDB(false, false);
                c.getPlayer().dropMessage("領取了 " + gain + " 點在線點數, 目前楓葉點數: " + c.getPlayer().getCSPoints(2));
            }
            return true;
        }

        @Override
        public String getMessage() {
            return new StringBuilder().append(ServerConstants.PlayerGMRank.普通玩家.getCommandPrefix()).append("jcds - 領取在線點數").toString();
        }
    }

    public static class dice extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

            int a = Randomizer.rand(1, 6);
            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.yellowChat("[骰點開牌][ "  + c.getPlayer().getName() + " ]  骰出 ["+a+"]點"));

            return true;
        }

        @Override
        public String getMessage() {
            return new StringBuilder().append("@dice - 單骰").toString();
        }
    }
  
     public static class dice3 extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

            int a = Randomizer.rand(1, 6);
            int a2 = Randomizer.rand(1, 6);
            int a3 = Randomizer.rand(1, 6);            
            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.yellowChat("[骰點開牌][ "  + c.getPlayer().getName() + " ]  骰出點數 ["+ a + "、"  + + a2 + "、" +  + a3 +"] 總和 ["+ (a+a2+a3) +"]點 "));

            return true;
        }

        @Override
        public String getMessage() {
            return new StringBuilder().append("@dice3 - 3科骰").toString();
        }
    }
 
    public static class Vac extends CommandExecute
    {
        @Override
        public boolean execute(final MapleClient c, final String[] splitted) {
            MapleMap.寵物吸物開關 = !MapleMap.寵物吸物開關;
            c.getPlayer().dropMessage(6, "目前寵吸狀態:" + (MapleMap.寵物吸物開關 ? "啟動" : "關閉"));
            return true;
        }
        
        @Override
        public String getMessage() {
            return new StringBuilder().append("!Vac  - 寵吸開關").toString();
        }
    }

     public static class 轉蛋綠廣 extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            c.getPlayer().setGachaponmega();
            return true;
        }

        @Override
        public String getMessage() {
            return new StringBuilder().append("@轉蛋綠廣 - 開/關閉轉蛋廣播").toString();
        }
    }
     
     public static class 在線點數 extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            int gain = c.getPlayer().getMP();
            if (gain <= 0) {
                c.getPlayer().dropMessage("目前沒有任何在線點數唷。");
                return true;
            }
            if (splitted.length < 2) {
                c.getPlayer().dropMessage("目前楓葉點數: " + c.getPlayer().getCSPoints(2));
                c.getPlayer().dropMessage("目前在線點數已經累積: " + gain + " 點，若要領取請輸入 @在線點數 是");
            } else if ("是".equals(splitted[1])) {
                gain = c.getPlayer().getMP();
                c.getPlayer().modifyCSPoints(2, gain, true);
                c.getPlayer().setMP(0);
                c.getPlayer().saveToDB(false, false);
                c.getPlayer().dropMessage("領取了 " + gain + " 點在線點數, 目前楓葉點數: " + c.getPlayer().getCSPoints(2));
            }
            return true;
        }

        @Override
        public String getMessage() {
            return new StringBuilder().append(ServerConstants.PlayerGMRank.普通玩家.getCommandPrefix()).append("在線點數 - 領取在線點數").toString();
        }
    }
    
/*    public static class dpm extends CommandExecute {

        @Override
        public boolean execute(final MapleClient c, String[] splitted) {
            if ((c.getPlayer().getMapId() == 910000000 && c.getPlayer().getLevel() >= 10) || c.getPlayer().isGM()) {
                if (!c.getPlayer().isTestingDPS()) {
                    c.getPlayer().toggleTestingDPS();
                    c.getPlayer().dropMessage(5, "請持續攻擊怪物10秒，來測試您的每秒輸出！");
                    final MapleMonster mm = MapleLifeFactory.getMonster(9001007);
                    int distance = ((c.getPlayer().getJob() >= 300 && c.getPlayer().getJob() < 413) || (c.getPlayer().getJob() >= 1300 && c.getPlayer().getJob() < 1500) || (c.getPlayer().getJob() >= 520 && c.getPlayer().getJob() < 600)) ? 125 : 50;
                    Point p = new Point(c.getPlayer().getPosition().x - distance, c.getPlayer().getPosition().y);
                    mm.setBelongTo(c.getPlayer());
                    final long newhp = Long.MAX_VALUE;
                    OverrideMonsterStats overrideStats = new OverrideMonsterStats();
                    overrideStats.setOHp(newhp);
                    mm.setHp(newhp);
                    mm.setOverrideStats(overrideStats);
                    c.getPlayer().getMap().spawnMonsterOnGroundBelow(mm, p);
                    final MapleMap nowMap = c.getPlayer().getMap();
                    Timer.EventTimer.getInstance().schedule(new Runnable() {
                        @Override
                        public void run() {
                            long health = mm.getHp();
                            nowMap.killMonster1(mm);
                            long dps = (newhp - health) / 15;
                            if (dps > c.getPlayer().getDPS()) {
                                c.getPlayer().dropMessage(6, "你的DPM是 " + dps + ". 這是一個新的紀錄！");
                                c.getPlayer().setDPS(dps);
                                c.getPlayer().savePlayer();
                                c.getPlayer().toggleTestingDPS();
                            } else {
                                c.getPlayer().dropMessage(6, "你的DPM是 " + dps + ". 您目前的紀錄是 " + c.getPlayer().getDPS() + ".");
                                c.getPlayer().toggleTestingDPS();
                            }

                        }
                    }, 60000);
                } else {
                    c.getPlayer().dropMessage(5, "請先把你的這回DPM測試完畢。");
                    return true;
                }
                
            } else {
                c.getPlayer().dropMessage(5, "只能在自由市場測試DPM，並且等級符合10以上。");
                return true;
            }
            return true;
        }

        @Override
        public String getMessage() {
            return new StringBuilder().append(ServerConstants.PlayerGMRank.普通玩家.getCommandPrefix()).append("dpm - 測試稻草人").toString();
        }
    }*/
     

 }
