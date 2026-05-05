/*
 少林妖僧 -- 入口NPC
 */

var status = -1;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1 || (status >= 0 && mode == 0)) {
        cm.dispose();
        return;
    }
    if (mode == 1) status++;
    else status--;

    if (status == 0) {
        cm.sendYesNo("前方是#r武林妖僧#k的巢穴。\r\n是否要進入挑戰？");
    } else if (status == 1) {
        var em = cm.getEventManager("shaoling");
        if (em == null) {
            cm.sendOk("當前副本有問題，請聯絡管理員。");
            cm.dispose();
            return;
        }
        var prop = em.getProperty("state");
        if (prop != null && !prop.equals("0")) {
            cm.sendOk("裡面已經有人在挑戰，請稍後再試。");
            cm.dispose();
            return;
        }
        em.startInstance(cm.getParty(), cm.getMap());
        cm.dispose();
    }
}
