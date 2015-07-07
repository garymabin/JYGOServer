package cn.garymb.ygoserver.ygo;

import java.security.InvalidParameterException;

import cn.garymb.ygoserver.util.TextUtils;

public class MyCardStyleGameConfig extends GameConfig {

	public MyCardStyleGameConfig(String info) {
		create(info);
	}

	public MyCardStyleGameConfig(YGOGamePacket packet) {
		create(packet);
	}

	@Override
	public void create(YGOGamePacket packet) {
		try {
			lflist = packet.readInt32();
			rule = packet.readByte();
			mode = packet.readByte();
			enablePriority = packet.readByte() > 0;
			noCheckDeck = packet.readByte() > 0;
			noShuffleDeck = packet.readByte() > 0;
			packet.readByte();
			packet.readByte();
			packet.readByte();
			startLp = packet.readInt32();
			startHand = packet.readByte();
			drawCount = packet.readByte();
			gameTimer = packet.readInt16();
			packet.read(20);
			name = new String(packet.read(30), "UTF-8");
			if (TextUtils.isEmpty(name)) {
				name = GameManager.randomGameName();
			}
			gameTimer = 120;
		} catch (Exception e) {
			mode = 0;
			lflist = 0;
			rule = 2;
			enablePriority = false;
			noCheckDeck = false;
			noShuffleDeck = false;
			startLp = 8000;
			startHand = 5;
			drawCount = 1;
			gameTimer = 120;
			name = GameManager.randomGameName();
		}
	}

	@Override
	public void create(String info) {
		try {
			if (info.startsWith("M#")) {
				mode = 1;
				lflist = 0;
				rule = 2;
				enablePriority = false;
				noCheckDeck = false;
				noShuffleDeck = false;
				startLp = 8000;
				startHand = 5;
				drawCount = 1;
				gameTimer = 120;
				name = info.substring(2, info.length() - 2);
			} else if (info.startsWith("T#")) {
				mode = 2;
				lflist = 0;
				rule = 2;
				enablePriority = false;
				noCheckDeck = false;
				noShuffleDeck = false;
				startLp = 8000;
				startHand = 5;
				drawCount = 1;
				gameTimer = 120;
				name = info.substring(2, info.length() - 2);
			} else {
				String[] paramSegs = info.split(",");
				if (paramSegs != null && paramSegs.length == 4) {
					if (paramSegs[0].length() >= 6) {
						String configStr = paramSegs[0];
						rule = Integer.parseInt(configStr.substring(0, 1));
						mode = Integer.parseInt(configStr.substring(1, 2));
						enablePriority = configStr.substring(2, 3).equals("T");
						noCheckDeck = configStr.substring(3, 4).equals("T");
						noShuffleDeck = configStr.substring(4, 5).equals("T");
						startLp = Integer.parseInt(configStr.substring(5, configStr.length()));
						startHand = Integer.parseInt(paramSegs[1]);
						drawCount = Integer.parseInt(paramSegs[2]);
						name = paramSegs[3];
						gameTimer = 120;
						lflist = 0;
					} else {
						throw new InvalidParameterException();
					}
				} else {
					mode = 0;
					lflist = 0;
					rule = 2;
					enablePriority = false;
					noCheckDeck = false;
					noShuffleDeck = false;
					startLp = 8000;
					startHand = 5;
					drawCount = 1;
					gameTimer = 120;
					name = info;
				}
			}
		} catch (Exception e) {
			mode = 0;
			lflist = 0;
			rule = 2;
			enablePriority = false;
			noCheckDeck = false;
			noShuffleDeck = false;
			startLp = 8000;
			startHand = 5;
			drawCount = 1;
			gameTimer = 120;
			name = info;
		}
	}
}
