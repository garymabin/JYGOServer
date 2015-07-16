package cn.garymb.ygoserver.ygo;

import java.util.ArrayList;
import java.util.List;

import edu.emory.mathcs.backport.java.util.Collections;

public class GameRoom {

	private Game mGame;
	private List<GameClient> mClients;

	public volatile boolean isOpen;
	private boolean mClosePending;

	@SuppressWarnings("unchecked")
	public GameRoom(GameConfig config) {
		mClients = Collections.synchronizedList(new ArrayList<GameClient>());
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
		for (GameClient client : mClients) {
			// FIXME
			client.forceStop();
		}
	}

	public void chat(GameClient client, byte[] msg) {
		if (msg == null) {
			return;
		}
		mGame.chat(client.getBoundedPlayer(), msg);
	}

	public void moveToDuelList(GameClient client) {
		mGame.moveToDuelList(client);
	}

	public void moveToObserver(GameClient client) {
		mGame.moveToObserver(client);
	}

	public void removePlayer(GameClient client) {
		mClients.remove(client);
		client.forceStop();
		mGame.removePlayer(client.getBoundedPlayer());
	}

	public void setReady(GameClient client, boolean b) {
		mGame.setReady(client, b);
	}

	public void kick(GameClient client, int pos) {
		mGame.kick(client, pos);
	}

	public void startDuel(GameClient client) {
		mGame.startDuel(client);
	}

	public void handResult(GameClient client, int res) {
		mGame.handResult(client, res);
	}

	public void tpResult(GameClient client, boolean tp) {
		mGame.tpResult(client, tp);
	}
}
