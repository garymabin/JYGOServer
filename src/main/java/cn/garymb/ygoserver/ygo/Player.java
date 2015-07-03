package cn.garymb.ygoserver.ygo;

import java.lang.ref.WeakReference;

public class Player {
	
	private WeakReference<GameClient> mAttachedClient;

	public Player(GameClient gameClient) {
		mAttachedClient = new WeakReference<GameClient>(gameClient);
	}

}
