package cn.garymb.ygoserver.ygo;

public abstract class GameConfig {
	public int lflist;
	public int rule;
	public int mode;
	public boolean enablePriority;
	public boolean noCheckDeck;
	public boolean  noShuffleDeck;
	public int startLp;
	public int startHand;
	public int drawCount;
	public int gameTimer;
	public String name;
	public abstract void create(String info);
	public abstract void create(YGOGamePacket packet);
}
