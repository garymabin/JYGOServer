package cn.garymb.ygoserver.ygo;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.garymb.ygoserver.server.Packet;
import cn.garymb.ygoserver.util.TimerTask;
import cn.garymb.ygoserver.ygo.ocgwrapper.Duel;
import cn.garymb.ygoserver.ygo.ocgwrapper.type.GameMessage;
import cn.garymb.ygoserver.ygo.ocgwrapper.type.PlayerChange;
import cn.garymb.ygoserver.ygo.ocgwrapper.type.PlayerType;

public class Game {

	private GameState mState;
	private GameConfig mConfig;
	private Player[] mPlayers;
	private boolean[] mIsReady;
	private boolean isTag;
	private boolean isMatch;
	private List<Player> mObservers = new ArrayList<Player>();
	private WeakReference<GameRoom> mAttachedGameRoom;

	private int[] mMatchResult = new int[3];
	private volatile int mDuelCount = 0;
	private volatile int mStartPlayer;
	private volatile boolean mSwapped = false;
	private Player mHostPlayer;

	private volatile boolean mMatchKill = false;

	private Duel mDuel;
	private Replay mReplay;
	
	private TimerTask mChangeSideTask = null;

	public Game(GameRoom gameRoom, GameConfig config) {
		mConfig = config;
		mAttachedGameRoom = new WeakReference<GameRoom>(gameRoom);
		mState = GameState.Lobby;
		isMatch = config.mode == 1;
		isTag = config.mode == 2;
		mPlayers = new Player[isTag ? 4 : 2];
		mIsReady = new boolean[isTag ? 4 : 2];
	}

	private void sendToAll(Packet p) {
		sendToPlayers(p);
		sendToObservers(p);
	}

	private void sendToAllBut(Packet packet, Player exceptions) {
		for (Player p : mPlayers) {
			if (p.equals(exceptions)) {
				continue;
			}
			GameClient c = p.getAttachedClient();
			if (c != null) {
				c.addPacketToSend(packet);
			}
		}
		for (Player p : mObservers) {
			if (p.equals(exceptions)) {
				continue;
			}
			GameClient c = p.getAttachedClient();
			if (c != null) {
				c.addPacketToSend(packet);
			}
		}
	}

	private void sendToPlayers(Packet packet) {
		for (Player p : mPlayers) {
			GameClient c = p.getAttachedClient();
			if (c != null) {
				c.addPacketToSend(packet);
			}
		}
	}

	private void sendToObservers(Packet packet) {
		for (Player p : mObservers) {
			GameClient c = p.getAttachedClient();
			if (c != null) {
				c.addPacketToSend(packet);
			}
		}
	}

	private int getAvailablePlayerPos() {
		for (int i = 0; i < mPlayers.length; i++) {
			if (mPlayers[i] == null)
				return i;
		}
		return -1;
	}

	public Player getHostPlayer() {
		return mHostPlayer;
	}

	/* package */ void setHostPlayer(Player mHostPlayer) {
		this.mHostPlayer = mHostPlayer;
	}

	public void moveToDuelList(GameClient client) {
		Player p = client.getBoundedPlayer();
		synchronized (this) {
			if (!mState.equals(GameState.Lobby)) {
				return;
			}
			int pos = getAvailablePlayerPos();
			if (pos == -1) {
				return;
			}
			if (p.getType() == PlayerType.Observer.intValue()) {
				int typeValue = p.getType();
				if (isTag || mIsReady[typeValue]) {
					return;
				}
				pos = (typeValue + 1) % 4;
				while (mPlayers[pos] != null) {
					pos = (pos + 1) % 4;
				}
				YGOGamePacket change = new YGOGamePacket(GameMessage.STOC_HS_PLAYER_CHANGE);
				change.writeByte((typeValue << 4) + pos);
				sendToAll(change);

				mPlayers[typeValue] = null;
				mPlayers[pos] = p;
				p.setType(pos);
				p.sendTypeChange(p.equals(mHostPlayer));
			} else {
				mObservers.remove(p);
				mPlayers[pos] = p;
				p.setType(pos);

				YGOGamePacket enter = new YGOGamePacket(GameMessage.STOC_HS_PLAYER_ENTER);
				enter.write(Arrays.copyOf(p.getName().getBytes(), 20));
				enter.writeByte(pos);
				sendToAll(enter);

				YGOGamePacket nwatch = new YGOGamePacket(GameMessage.STOC_HS_WATCH_CHANGE);
				nwatch.writeInt16(mObservers.size());
				sendToAll(nwatch);
				p.sendTypeChange(p.equals(mHostPlayer));
			}
		}
	}

	public void chat(Player player, byte[] msg) {
		YGOGamePacket p = new YGOGamePacket(GameMessage.CTOS_CHAT);
		p.writeInt16(player.getType());
		p.write(msg);
		sendToAllBut(p, player);
	}

	public void moveToObserver(GameClient client) {
		Player p = client.getBoundedPlayer();
		synchronized (this) {
			if (!mState.equals(GameState.Lobby)) {
				return;
			}
			int typeValue = p.getType();
			if (typeValue == PlayerType.Observer.intValue()) {
				return;
			}
			if (mIsReady[typeValue]) {
				return;
			}
			mPlayers[typeValue] = null;
			mIsReady[typeValue] = false;
			mObservers.add(p);

			YGOGamePacket change = new YGOGamePacket(GameMessage.STOC_HS_PLAYER_CHANGE);
			change.writeByte((p.getType() << 4) + PlayerChange.Observe.intValue());
			sendToAll(change);

			p.setType(PlayerType.Observer.intValue());
			p.sendTypeChange(p.equals(mHostPlayer));
		}

	}

	public void removePlayer(Player boundedPlayer) {
		synchronized (this) {
			if (boundedPlayer.equals(mHostPlayer) && mState.equals(GameState.Lobby)) {
				GameRoom gr = mAttachedGameRoom.get();
				if (gr != null) {
					gr.close();
				}
			} else if (boundedPlayer.getType() == PlayerType.Observer.intValue()) {
				mObservers.remove(boundedPlayer);
				if (mState.equals(GameState.Lobby)) {
					YGOGamePacket nwatch = new YGOGamePacket(GameMessage.STOC_HS_WATCH_CHANGE);
					nwatch.writeInt16(mObservers.size());
					sendToAll(nwatch);
				}
			} else if (mState.equals(GameState.Lobby)) {
				int typeValue = boundedPlayer.getType();
				mPlayers[typeValue] = null;
				mIsReady[typeValue] = false;

				YGOGamePacket change = new YGOGamePacket(GameMessage.STOC_HS_PLAYER_CHANGE);
				change.writeByte((typeValue << 4) + PlayerChange.Leave.intValue());
				sendToAll(change);
			} else {
				surrender(boundedPlayer, 4, true);
			}
		}
	}

	private void surrender(Player player, int reason, boolean force) {
		synchronized (this) {
			if (!force) {
				if (!mState.equals(GameState.Duel)) {
					return;
				}
			}
			if (player.getType() == PlayerType.Observer.intValue()) {
				return;
			}
			YGOGamePacket win = new YGOGamePacket(GameMessage.WIN);
			int typeValue = player.getType();
			if (isTag) {
				typeValue = player.getType() >= 2 ? 1 : 0;
			}
			win.writeByte(1 - typeValue);
			win.writeByte(reason);
			sendToAll(win);

			matchSaveResult(1 - typeValue);
			endDuel(reason == 4);
		}
	}

	private void endDuel(boolean force) {
		if (mState == GameState.Duel) {
			if (mReplay != null && !mReplay.isDisabled()) {
				mReplay.end();
				byte[] replayData = mReplay.getSavedData();
				ReplaySavePacket rsp = new ReplaySavePacket();
				rsp.write(replayData);
				sendToAll(rsp);
			}
			mState = GameState.End;
			if (mDuel != null) {
				mDuel.end();
			}
		}
		if (mSwapped) {
			mSwapped = false;
			Player temp = mPlayers[0];
			mPlayers[0] = mPlayers[1];
			mPlayers[1] = temp;
			mPlayers[0].setType(0);
			mPlayers[0].setType(1);
		}
		if (isMatch && !force && !matchIsEnd()) {
			mIsReady[0] = false;
			mIsReady[1] = false;
			serverMessage("You have 120 seconds to side");
			mState = GameState.Side;
			sendToPlayers(new YGOGamePacket(GameMessage.STOC_CHANGE_SIDE));
			sendToObservers(new YGOGamePacket(GameMessage.STOC_WAITING_SIDE));
			mChangeSideTask = new TimerTask() {
				
				public void run() {
					
				}
			};
			GameManager.submit(mChangeSideTask, 120 * 1000);
		}
	}

	private void serverMessage(String msg) {
		String finalmsg = "[Server] " + msg;
		try {
			YGOGamePacket p = new YGOGamePacket(GameMessage.STOC_CHAT);
			p.writeInt16(PlayerType.Yellow.intValue());
			p.write(finalmsg.getBytes("UTF-8"));
			sendToAll(p);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	private void matchSaveResult(int player) {
		synchronized (mMatchResult) {
			if (!isMatch)
				return;
			if (player < 2 && mSwapped)
				player = 1 - player;
			if (player < 2)
				mStartPlayer = 1 - player;
			else
				mStartPlayer = 1 - mStartPlayer;
			if (mDuelCount >= 0 && mDuelCount <= 2) {
				mMatchResult[mDuelCount++] = player;
			}
		}
	}

	private void matchKill() {
		mMatchKill = true;
	}

	public boolean matchIsEnd() {
		synchronized (mMatchResult) {
			if (mMatchKill) {
				return true;
			}
			int[] wins = new int[3];
			for (int i = 0; i < mDuelCount; i++)
				wins[mMatchResult[i]]++;
			return wins[0] == 2 || wins[1] == 2 || wins[0] + wins[1] + wins[2] == 3;
		}
	}
}
