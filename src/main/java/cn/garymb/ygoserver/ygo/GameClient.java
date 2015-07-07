package cn.garymb.ygoserver.ygo;

import java.lang.ref.WeakReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GameClient extends YGOIOService<Object> {
	
	private static final Logger log = Logger.getLogger(GameClient.class.getName());
	
	private Player mPlayer = null;
	private WeakReference<GameRoom> mBoundedGameRoom = null;
	
	private volatile boolean isGameBounded = false;
	
	public GameClient() {
		mPlayer = new Player(this);
	}
	
	public Player getBoundedPlayer() {
		return mPlayer;
	}

	public GameRoom getBoundedGameRoom() {
		return mBoundedGameRoom.get();
	}

	/*package*/ boolean setBoundedGameRoom(GameRoom game) {
		if (!isGameBounded) {
			this.mBoundedGameRoom = new WeakReference<GameRoom>(game);
			isGameBounded = true;
			return true;
		} else {
			log.log(Level.WARNING, "why game client is bounded to a game more than once ?");
			return false;
		}
	}
	
}
