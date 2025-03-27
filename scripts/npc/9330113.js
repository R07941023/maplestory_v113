/* 
 * NPC   : Dev Doll
 * Map   : GMMAP
 */

var status = 0;
var invs = Array(2, 5);
var invv;
var selected;
var slot_1 = Array();
var slot_2 = Array();
var statsSel;

function start() {
	//action(1,0,0);
	cm.sendSimple("我可以幫助你刪除任何物品: \r\n\r\n#b#L1#裝備欄 \r\n#b#L2#消耗欄 \r\n#b#L3#裝飾欄 \r\n#b#L4#其他欄 \r\n#b#L5#特殊欄");
}

function action(mode, type, selection) {
	if (mode != 1) {
		cm.dispose();
		return;
	}
	if (selection == 1) 
	{
		var invs = Array(1, 5);
	}
	else if (selection == 2)
	{
		var invs = Array(2, 5);
	}
	else if (selection == 3)
	{
		var invs = Array(3, 5);
	}
	else if (selection == 4)
	{
		var invs = Array(4, 5);
	}
	else if (selection == 5)
	{
		var invs = Array(5, 5);
	}
	else
	{
		invs = Array(3, 5);
	}
	status++;
	if (status == 1) {
		var bbb = false;
		var selStr = "我可以幫助你刪除物品:\r\n\r\n#b";
    var hashset = Array();
		for (var x = 0; x < invs.length; x++) {
			var inv = cm.getInventory(invs[x]);
			for (var i = 0; i <= inv.getSlotLimit(); i++) {
				if (x == 0) {
					slot_1.push(i);
				} else {
					slot_2.push(i);
				}
				var it = inv.getItem(i);
				if (it == null) {
					continue;
				}
				var itemid = it.getItemId();
				if (selection != 5 && cm.isCash(itemid)) {
					continue;
				}
        if (hashset.indexOf(itemid) != -1){
          continue;
        }
        hashset.push(itemid);
				bbb = true;
				selStr += "#L" + (invs[x] * 1000 + i) + "##t" + itemid + "##l\r\n";
			}
		}
		if (!bbb) {
			cm.sendOk("You don't have any non-cash items.");
			cm.dispose();
			return;
		}
		cm.sendSimple(selStr + "#k");
	} else if (status == 2) {
		invv = selection / 1000;
		selected = selection % 1000;
		var inzz = cm.getInventory(invv);
		
		if (invv == invs[0]) {
			statsSel = inzz.getItem(slot_1[selected]);
		} else {
			statsSel = inzz.getItem(slot_2[selected]);
		}
		if (statsSel == null) {
			cm.sendOk("錯誤，請再嘗試");
			cm.dispose();
			return;
		}
		cm.gainItem(statsSel.getItemId(), -1); 
		cm.sendOk("你刪除物品為 #t" + statsSel.getItemId());
		cm.dispose();
	}
}
/*
var status = 6;

function start() {
    action(0,0,0);
}

function cancelled() {
    action(0,0,0);
}

function action(mode, type, selection) {
    switch (status) {
	case 5:
	    status = 6;
	    cm.sendNext("I'm afraid I can't let you go without entering the right password. (Yes I'm bad, blame me by all means) #bYou Cheater! I'm not stupid either.")
	    break;
	case 6:
	    status = 10;
	    cm.sendGetText("For the sake of privacy, please enter your first password which you use to login. #bYou have 2 tries from now.");
	    break;
	case 10: {
	    var pw = cm.getText();
	    if (cm.checkPassword(pw)) {
		cm.sendOk("You have authenticated yourself successfully, enjoy. Celino Online Staff. #b(Please do set a second password on the login page too)");
		cm.dispose();
	    } else {
		cm.sendOk("Invalid password, please try again.");
		status = 6;
	    }
	    break;
	}
    }
}*/

