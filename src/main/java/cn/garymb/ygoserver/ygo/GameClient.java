package cn.garymb.ygoserver.ygo;

public class GameClient extends YGOIOService<Object> {
	
	private Player mPlayer;
	
	public GameClient() {
		mPlayer = new Player(this);
	}
	
	public Player getBoundedPlayer() {
		return mPlayer;
	}
	
}
