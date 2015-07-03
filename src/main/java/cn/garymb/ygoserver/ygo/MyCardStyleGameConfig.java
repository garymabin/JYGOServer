package cn.garymb.ygoserver.ygo;

import java.security.InvalidParameterException;

public class MyCardStyleGameConfig extends GameConfig {

	public MyCardStyleGameConfig(String info) {
		load(info);
	}

	@Override
	public void load(String info) {
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
