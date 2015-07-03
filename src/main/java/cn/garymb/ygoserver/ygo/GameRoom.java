package cn.garymb.ygoserver.ygo;

import java.util.LinkedList;
import java.util.List;

public class GameRoom {
	private Game mGame;
	private List<GameClient> mClients;
	
	private boolean isOpen;
	private boolean mClosePending;
	
	public GameRoom(GameConfig config)  {
		mClients = new LinkedList<GameClient>();
		mGame = new Game(this, config);
		isOpen = true;
	}
	
	public void addClient(GameClient client) {
		mClients.add(client);
	}
	
	public void removeClient(GameClient client) {
		mClients.remove(client);
	}
	
	public void close() {
		isOpen = false;
		for(GameClient client : mClients) {
			//FIXME
			client.forceStop();
		}
	}
}
