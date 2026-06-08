/*
 * 轉蛋機 NPC
 * 2022/06/19 優化版
 */
var status = -1;
var req = [5220000, 1];

// 獎池
var erasers = [4001197, 4001116, 4001038, 4001039, 4001040, 4001041, 4001042, 4001043, 4001115];
var lottery = [4031365]
var maple = [4001126]
var scroll = [2340000]
// var itemList = [].concat(erasers);
var itemList = [].concat(erasers, lottery, maple, scroll);

function start() {
  action(1, 0, 0);
}

var rewards = ""; // 獎品列表
var times = 0; // 消耗的轉蛋卷數
var haveSpace = true; // 還有沒有空間
var haveItem = true; // 還有沒有轉蛋卷
var stop; // result = -1;  => SRC : gainGachaponItem 方法裡面的 addbyId_Gachapon 如果有某個類別欄位已滿會回傳 -1

/* 重置 */
function init() {
  rewards = "";
  times = 0;
  haveSpace = true;
  haveItem = true;
}

/* 轉蛋 */
function draw() {
  var result;
  var random = Math.floor(Math.random() * itemList.length); // 產生亂數抽取獎池其中一個道具
  var item = itemList[random]; // 抽中的道具
  
  if (cm.getPlayer().itemQuantity(req[0]) - times < 1) {
    // 情況1: 剩餘轉蛋卷不足
    haveItem = false;
  } else if (!cm.canHold()) {
    // 情況2: 剩餘空間不足
    haveSpace = false;
  } else {
    result = cm.gainItem(item, 1);
    if (result === -1) {
      stop = true;
    } else {
      rewards += "#i" + item + "#";
      times++;
    }
  }
}

/**
 * 結算
 * @param {*} type 0: 單抽 1: 十抽
 */
function finish(type) {
  if (times > 0) {
    var msg = type === 1 ? "\r\n#L996##b繼續十抽#l　#L997##b前往單抽#l" : "\r\n#L997##b繼續單抽#l　#L996##b前往十抽#l";
    cm.gainItem(req[0], -times);
    cm.sendNext("恭喜獲得\r\n" + rewards + msg + "\r\n#L998##r結束轉蛋#l　#L999##d返回上一頁#l");
  } else if (!haveSpace || stop) {
    cm.sendOk("不好意思，請確認您的背包是否有空位。");
  } else if (!haveItem) {
    cm.sendOk("不好意思，您身上的#b#t" + req[0] + "##i" + req[0] + "##k不足！");
  }
  init();
}

function action(mode, type, selection) {
  if (mode == 1) {
    status++;
  } else {
    cm.dispose();
    return;
  }

  if (status === 0) {
    /* 檢查身上是否有轉蛋卷 */
    if (cm.haveItem(req[0], req[1])) {
      var msg = "您身上有#b#t" + req[0] +"##i" + req[0] + "##k可以進行轉蛋。\r\n你確定要使用 #b#p" + cm.getNpc() + "##k 進行轉蛋嗎?\r\n #L0#我要單抽！ #l \r\n #L1#我要一次抽#r十個#k！#l \r\n #L2##b查看轉蛋機內容物！#l";
      cm.sendYesNo(msg);
    } else {
      cm.sendOk("不好意思！您沒有#b#t" + req[0] + "##i" + req[0] + "##k，無法進行轉蛋。");
      cm.dispose();
    }
  } else if (status === 1) {
    /* 根據選擇擇的選項做對應的事情 */
    switch (selection) {
      // 單抽
      case 0:
        if (!stop && haveSpace) {
          draw();
        }
        finish(0);
        break;
      // 十抽
      case 1:
        for (var counts = 0; counts < 10; counts++) {
          if (stop || !haveSpace) {
            break;
          }
          draw();
        }
        finish(1);
        break;
      // 確認獎池
      case 2:
        // cm.sendOk("請用轉蛋券揭開秘密!");
        var msg = "";
        for (var i = 0; i < itemList.length; i++) {
          if (i % 8 == 0) {
            msg += "\r\n";
          }
          msg += "#i" + itemList[i] + "#";
        }
        cm.sendSimple("" + msg + "\r\n #L999##r退回上一頁#l");
        break;
    }
  } else if (status === 2) {
    switch (selection) {
      // 繼續十抽
      case 996:
        status = 0;
        action(1, 0, 1);
        break;
      // 繼續單抽
      case 997:
        status = 0;
        action(1, 0, 0);
        break;
      // 退出轉蛋
      case 998:
        cm.dispose();
        break;
      // 退回菜單重選
      case 999:
        status = -1;
        action(1, 0, 0);
        break;
      default:
        cm.dispose();
        break;
    }
  } else {
    cm.dispose();
  }
}