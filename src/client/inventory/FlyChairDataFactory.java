/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import provider.MapleData;
import provider.MapleDataDirectoryEntry;
import provider.MapleDataFileEntry;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import tools.Pair;

public class FlyChairDataFactory {

    private static final MapleDataProvider dataRoot = MapleDataProviderFactory.getDataProvider("Item.wz");
    private static final Map<Integer, Integer> FlyChair = new HashMap<>();
    private static List<Integer> TamingMob = new ArrayList<>();

    public static final int getFlyChair(final int chairid) {
        int mob = 0;
        if (FlyChair.get(chairid) == null) {
            final MapleData ChairData = dataRoot.getData("Install/0301.img");
            int tamingmob = 0;
            String chair = "0" + String.valueOf(chairid);
            if (ChairData != null) {
                tamingmob = MapleDataTool.getInt(chair + "/info/tamingMob", ChairData, 0);
            }
            FlyChair.put(chairid, tamingmob);
            mob = FlyChair.get(chairid);
        } else {
            mob = FlyChair.get(chairid);
        }
        if (!TamingMobExist(mob)) {
            mob = 0;
        }
        return mob;
    }

    public static boolean TamingMobExist(int effect) {
        if (TamingMob.isEmpty()) {
            MapleDataProvider data = MapleDataProviderFactory.getDataProvider("Character.wz/TamingMob");
            MapleDataDirectoryEntry root = data.getRoot();
            for (MapleDataFileEntry topDir : root.getFiles()) {
                int id = Integer.parseInt(topDir.getName().substring(0, 8));
                TamingMob.add(id);
            }
        }
        return TamingMob.contains(effect);
    }
}
