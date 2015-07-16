package cn.garymb.ygoserver.ygo;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import cn.garymb.ygoserver.server.Packet;
import cn.garymb.ygoserver.util.TimerTask;
import cn.garymb.ygoserver.ygo.ocgwrapper.Duel;
import cn.garymb.ygoserver.ygo.ocgwrapper.type.GameMessage;
import cn.garymb.ygoserver.ygo.ocgwrapper.type.PlayerChange;
import cn.garymb.ygoserver.ygo.ocgwrapper.type.PlayerType;

public class Game {

	public class SideEndTimeTask extends TimerTask {

		public void run() {
			synchronized (Game.this) {
				if (!mIsReady[0] && !mIsReady[1]) {
					mState = GameState.End;
					end();
					return;
				}
				surrender(!mIsReady[0] ? mPlayers[0] : mPlayers[1], 3, true);
				mState = GameState.End;
				end();
			}
		}

	}

	public class SideAlarmTimeTask extends TimerTask {

		private int[] mTimeLeft;

		public SideAlarmTimeTask(int... timeInSec) {
			mTimeLeft = timeInSec;
		}

		public void run() {
			serverMessage("You have " + mTimeLeft[0] + " seconds left.");
			int i = 0;
			while (++i < mTimeLeft.length) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				serverMessage("You have " + mTimeLeft[i] + " seconds left.");
			}
		}

	}

	private GameState mState;
	private GameConfig mConfig;
	private Player[] mPlayers;
	private Player[] mCurrentPlayers;
	private boolean[] mIsReady;
	private int[] mHandResult;
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
	private Banlist mBanlist;

	private TimerTask[] mChangeSideTasks = null;

	public Game(GameRoom gameRoom, GameConfig config) {
		mConfig = config;
		mAttachedGameRoom = new WeakReference<GameRoom>(gameRoom);
		mState = GameState.Lobby;
		isMatch = config.mode == 1;
		isTag = config.mode == 2;
		mPlayers = new Player[isTag ? 4 : 2];
		mCurrentPlayers = new Player[2];
		mIsReady = new boolean[isTag ? 4 : 2];
		mHandResult = new int[2];
		if (mConfig.lflist >= 0 && mConfig.lflist < BanlistManager.size()) {
			mBanlist = BanlistManager.get(mConfig.lflist);
		}
	}

	private void sendToTeam(Packet packet, int team) {
		if (!isTag) {
			mPlayers[team].getAttachedClient().addPacketToSend(packet);
		} else if (team == 0) {
			mPlayers[0].getAttachedClient().addPacketToSend(packet);
			mPlayers[1].getAttachedClient().addPacketToSend(packet);
		} else {
			mPlayers[2].getAttachedClient().addPacketToSend(packet);
			mPlayers[3].getAttachedClient().addPacketToSend(packet);
		}
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
			removePlayerUnLocked(boundedPlayer);
		}
	}

	private void removePlayerUnLocked(Player boundedPlayer) {
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

	private void surrender(Player player, int reason, boolean force) {
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
			mChangeSideTasks = new TimerTask[4];
			mChangeSideTasks[0] = new SideAlarmTimeTask(60);
			mChangeSideTasks[1] = new SideAlarmTimeTask(30);
			mChangeSideTasks[2] = new SideAlarmTimeTask(10);
			mChangeSideTasks[3] = new SideAlarmTimeTask(5, 4, 3, 2, 1);
			GameManager.submit(mChangeSideTasks[0], 60 * 1000);
			GameManager.submit(mChangeSideTasks[1], 90 * 1000);
			GameManager.submit(mChangeSideTasks[2], 110 * 1000);
			GameManager.submit(mChangeSideTasks[3], 115 * 1000);
		} else {
			end();
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

	private void end() {
		sendToAll(new YGOGamePacket(GameMessage.STOC_DUEL_END));
		// FIXME: how to perform close delayed;
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

	public void setReady(GameClient client, boolean ready) {
		Player boundedPlayer = client.getBoundedPlayer();
		synchronized (this) {
			if (!GameState.Lobby.equals(mState)) {
				return;
			}
			if (PlayerType.Observer.intValue() == boundedPlayer.getType()) {
				return;
			}
			if (ready) {
				boolean ocg = mConfig.rule == 0 || mConfig.rule == 2;
				boolean tcg = mConfig.rule == 1 || mConfig.rule == 2;
				int result = 1;
				if (mConfig.noCheckDeck) {
					result = 0;
				} else if (boundedPlayer.getDeck() != null) {
					result = boundedPlayer.getDeck().check(mBanlist, ocg, tcg);
				}
				if (result != 0) {
					YGOGamePacket rechange = new YGOGamePacket(GameMessage.STOC_HS_PLAYER_CHANGE);
					rechange.writeByte((boundedPlayer.getType() << 4) + PlayerChange.NotReady.intValue());
					client.addPacketToSend(rechange);
					YGOGamePacket error = new YGOGamePacket(GameMessage.STOC_ERROR_MSG);
					error.writeByte(2);
					for (int i = 0; i < 3; i++) {
						error.writeByte(0);
					}
					error.writeInt32(result);
					client.addPacketToSend(error);
					return;
				}
			}
			mIsReady[boundedPlayer.getType()] = ready;
			YGOGamePacket change = new YGOGamePacket(GameMessage.STOC_HS_PLAYER_CHANGE);
			change.writeByte((boundedPlayer.getType() << 4)
					+ (ready ? PlayerChange.Ready.intValue() : PlayerChange.NotReady.intValue()));
			sendToAll(change);
		}
	}

	public void kick(GameClient client, int pos) {
		Player boundedPlayer = client.getBoundedPlayer();
		synchronized (this) {
			if (GameState.Lobby.equals(mState)) {
				return;
			}
			if (pos >= mPlayers.length || !boundedPlayer.equals(mHostPlayer) || boundedPlayer.equals(mPlayers[pos])
					|| mPlayers[pos] == null) {
				return;
			}
			removePlayerUnLocked(mPlayers[pos]);
		}
	}

	public void startDuel(GameClient client) {
		Player boundedPlayer = client.getBoundedPlayer();
		synchronized (this) {
			if (!GameState.Lobby.equals(mState)) {
				return;
			}
			if (!boundedPlayer.equals(mHostPlayer)) {
				return;
			}
			for (int i = 0; i < mPlayers.length; i++) {
				if (!mIsReady[i]) {
					return;
				}
				if (mPlayers[i] == null) {
					return;
				}
			}
			mState = GameState.Hand;
			sendToAll(new YGOGamePacket(GameMessage.STOC_DUEL_START));
			sendHand();
		}
	}

	private void sendHand() {
		YGOGamePacket hand = new YGOGamePacket(GameMessage.STOC_SELECT_HAND);
		if (isTag) {
			GameClient c1 = mPlayers[0].getAttachedClient();
			if (c1 != null) {
				c1.addPacketToSend(hand);
			}
			GameClient c2 = mPlayers[2].getAttachedClient();
			if (c2 != null) {
				c2.addPacketToSend(hand);
			}
		} else {
			sendToPlayers(hand);
		}
	}

	public void handResult(GameClient client, int result) {
		Player boundedPlayer = client.getBoundedPlayer();
		synchronized (this) {
			if (!mState.equals(GameState.Hand)) {
				return;
			}
			if (boundedPlayer.getType() == PlayerType.Observer.intValue()) {
				return;
			}
			if (result < 1 || result > 3) {
				return;
			}
			if (isTag && boundedPlayer.getType() != 0 && boundedPlayer.getType() != 2) {
				return;
			}
			int type = boundedPlayer.getType();
			if (isTag && type == 2) {
				type = 1;
			}
			if (mHandResult[0] != 0 && mHandResult[1] != 0) {
				YGOGamePacket packet = new YGOGamePacket(GameMessage.STOC_HANDLE_RESULT);
				packet.writeByte(mHandResult[0]);
				packet.writeByte(mHandResult[1]);
				sendToTeam(packet, 0);
				sendToObservers(packet);

				packet = new YGOGamePacket(GameMessage.STOC_HANDLE_RESULT);
				packet.writeByte(mHandResult[1]);
				packet.writeByte(mHandResult[0]);
				sendToTeam(packet, 1);

				if (mHandResult[0] == mHandResult[1]) {
					mHandResult[0] = 0;
					mHandResult[1] = 0;
					sendHand();
					return;
				}

				if ((mHandResult[0] == 1 && mHandResult[1] == 2) || (mHandResult[0] == 2 && mHandResult[1] == 3)
						|| (mHandResult[0] == 3 && mHandResult[1] == 1)) {
					mStartPlayer = isTag ? 2 : 1;
				} else {
					mStartPlayer = 0;
				}
				mState = GameState.Starting;
				mPlayers[mStartPlayer].getAttachedClient().addPacketToSend(new YGOGamePacket(GameMessage.STOC_SELECT_TP));
			}
		}
	}

	public void tpResult(GameClient client, boolean tp) {
		Player boundedPlayer = client.getBoundedPlayer();
		synchronized (this) {
			if (GameState.Starting.equals(mState)) {
				return;
			}
			if (boundedPlayer.getType() != mStartPlayer) {
				return;
			}
			mSwapped = true;
			if (isTag) {
				Player temp = mPlayers[0];
				mPlayers[0] = mPlayers[2];
				mPlayers[2] = temp;
				
				mPlayers[0].setType(0);
				mPlayers[1].setType(1);
				mPlayers[2].setType(2);
				mPlayers[3].setType(3);
			} else {
				Player temp = mPlayers[0];
				mPlayers[0] = mPlayers[1];
				mPlayers[1] = temp;
				
				mPlayers[0].setType(0);
				mPlayers[1].setType(1);
			}
			mCurrentPlayers[0] = mPlayers[0];
			mCurrentPlayers[1] = mPlayers[isTag ? 3 : 1];
			
			mState = GameState.Duel;
			
			int seed = new Random(System.currentTimeMillis()).nextInt();
			mDuel = Duel.create(seed);
			Random rand = new Random(seed);
			mDuel.initPlayers(mConfig.startLp, mConfig.startHand, mConfig.drawCount);
			
			int opt = 0;
			if (mConfig.enablePriority) {
				opt += 0x08;
			}
			if (mConfig.noShuffleDeck) {
				opt += 0x10;
			}
			if (isTag) {
				opt += 0x20;
			}
			
			mReplay = new Replay((int)seed, isTag);
			try {
				mReplay.write(mPlayers[0].getName().getBytes("utf-8"));
				mReplay.write(mPlayers[1].getName().getBytes("utf-8"));
				if (isTag) {
					mReplay.write(mPlayers[2].getName().getBytes("utf-8"));
					mReplay.write(mPlayers[3].getName().getBytes("utf-8"));
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			mReplay.writeInt32(mConfig.startLp);
			mReplay.writeInt32(mConfig.startHand);
			mReplay.writeInt32(mConfig.drawCount);
			mReplay.writeInt32(opt);
			for (int i = 0; i < mPlayers.length; i++) {
				Player dplayer = mPlayers[i == 2 ? 3: (i == 3 ? 2 : i)];
				int pid = i;
				if (isTag) {
				}
			}
		}
	}
}
