package cn.garymb.ygoserver.ygo;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import cn.garymb.ygoserver.server.Packet;
import cn.garymb.ygoserver.util.TimerTask;
import cn.garymb.ygoserver.ygo.ocgwrapper.Duel;
import cn.garymb.ygoserver.ygo.ocgwrapper.type.CardLocation;
import cn.garymb.ygoserver.ygo.ocgwrapper.type.GameMessage;
import cn.garymb.ygoserver.ygo.ocgwrapper.type.PlayerChange;
import cn.garymb.ygoserver.ygo.ocgwrapper.type.PlayerType;

public class Game {
	
	public class HandEndTimeTask extends TimerTask {
		public void run() {
			synchronized (Game.this) {
				if (mState.equals(GameState.Hand)) {
					if (mHandResult[0] != 0) {
						surrender(mPlayers[1], 3, true);
					} else if (mHandResult[1] != 0){
						surrender(mPlayers[0], 3, true);
					} else {
						mState = GameState.End;
						end();
					}
					if (mHandResult[0] == 0 && mHandResult[1] == 0) {
						mState= GameState.End;
					} else {
						surrender(mPlayers[1- mLastResp], 3, true);
					}
				}
			}
		}

	}

	public class TpEndTimeTask extends TimerTask {
		public void run() {
			synchronized (Game.this) {
				if (mState.equals(GameState.Starting)) {
					surrender(mPlayers[mStartPlayer], 3, true);
					mState = GameState.End;
					end();
				}
			}
		}
	}

	public class SideEndTimeTask extends TimerTask {

		public void run() {
			synchronized (Game.this) {
				if (mState.equals(GameState.Side)) {
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

	}

	public class EchoAlarmTimeTask extends TimerTask {

		private int[] mTimeLeft;

		public EchoAlarmTimeTask(int... timeInSec) {
			mTimeLeft = timeInSec;
		}

		public void run() {
			serverMessage("You have " + mTimeLeft[0] + " seconds left.");
			int i = 0;
			while (++i < mTimeLeft.length && !Thread.interrupted()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
					break;
				}
				serverMessage("You have " + mTimeLeft[i] + " seconds left.");
			}
		}

	}

	private GameState mState;
	private GameConfig mConfig;
	private Player[] mPlayers;
	private Player[] mCurrentPlayers;
	private int mCurrentPlayer;
	private boolean[] mIsReady;
	private int[] mHandResult;
	private int[] mLifePoints;
	private boolean isTag;
	private boolean isMatch;
	private List<Player> mObservers = new ArrayList<Player>();
	private WeakReference<GameRoom> mAttachedGameRoom;

	private int[] mMatchResult = new int[3];
	private volatile int mDuelCount = 0;
	private volatile int mStartPlayer;
	private volatile boolean mSwapped = false;
	private Player mHostPlayer;
	private int mTurnCount;
	
	private int mLastResp;

	private IGameAnalyser mAnalyser;

	private boolean isTpSelect;
	private volatile boolean mMatchKill = false;

	private Duel mDuel;
	private Replay mReplay;
	private Banlist mBanlist;

	private TimerTask[] mHandTasks = null;
	private TimerTask[] mTpTasks = null;
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
		mLifePoints = new int[2];
		if (mConfig.lflist >= 0 && mConfig.lflist < BanlistManager.size()) {
			mBanlist = BanlistManager.get(mConfig.lflist);
		}
		mAnalyser = new YGODuelAnalyser(this);
	}

	private void sendToTeam(Packet packet, int team) {
		if (!isTag) {
			mPlayers[team].send(packet);
		} else if (team == 0) {
			mPlayers[0].send(packet);
			mPlayers[1].send(packet);
		} else {
			mPlayers[2].send(packet);
			mPlayers[3].send(packet);
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
			p.send(packet);
		}
		for (Player p : mObservers) {
			if (p.equals(exceptions)) {
				continue;
			}
			p.send(packet);
		}
	}

	private void sendToPlayers(Packet packet) {
		for (Player p : mPlayers) {
			p.send(packet);
		}
	}

	private void sendToObservers(Packet packet) {
		for (Player p : mObservers) {
			p.send(packet);
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
				try {
					enter.write(p.getName().getBytes("utf-8"), 0, 20);
				} catch (UnsupportedEncodingException e) {
					enter.write(p.getName().getBytes(), 0, 20);
					e.printStackTrace();
				}
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
			setSideTimer();
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
		setHandTimer();
		YGOGamePacket hand = new YGOGamePacket(GameMessage.STOC_SELECT_HAND);
		if (isTag) {
			mPlayers[0].send(hand);
			mPlayers[2].send(hand);
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
				mPlayers[mStartPlayer].send(new YGOGamePacket(GameMessage.STOC_SELECT_TP));
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

			mDuel.setAnalyser(mAnalyser);

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

			mReplay = new Replay((int) seed, isTag);
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
				Player dplayer = mPlayers[i == 2 ? 3 : (i == 3 ? 2 : i)];
				int pid = i;
				if (isTag) {
					pid = i >= 2 ? 1 : 0;
				}
				if (!mConfig.noShuffleDeck) {
					List<Integer> cards = new ArrayList<Integer>(60);
					Collections.copy(cards, dplayer.getDeck().main);
					Collections.shuffle(cards, rand);
					mReplay.writeInt32(cards.size());
					for (int id : cards) {
						if (isTag && (i == 1 || i == 3)) {
							mDuel.addTagCard(id, pid, CardLocation.Deck);
						} else {
							mDuel.addCard(id, pid, CardLocation.Deck);
						}
						mReplay.writeInt32(id);
					}
				} else {
					List<Integer> cards = dplayer.getDeck().main;
					mReplay.writeInt32(cards.size());
					for (int id : cards) {
						if (isTag && (i == 1 || i == 3)) {
							mDuel.addTagCard(id, pid, CardLocation.Deck);
						} else {
							mDuel.addCard(id, pid, CardLocation.Deck);
						}
						mReplay.writeInt32(id);
					}
				}
				mReplay.writeInt32(dplayer.getDeck().extra.size());
				for (int id : dplayer.getDeck().extra) {
					if (isTag && (i == 1 || i == 3)) {
						mDuel.addTagCard(id, pid, CardLocation.Extra);
					} else {
						mDuel.addCard(id, pid, CardLocation.Extra);
					}
					mReplay.writeInt32(id);
				}
			}
			YGOGamePacket packet = new YGOGamePacket(GameMessage.START);
			packet.writeByte(0);
			packet.writeInt32(mConfig.startLp);
			packet.writeInt32(mConfig.startLp);
			packet.writeInt16(mDuel.queryFieldCount(0, CardLocation.Deck));
			packet.writeInt16(mDuel.queryFieldCount(0, CardLocation.Extra));
			packet.writeInt16(mDuel.queryFieldCount(1, CardLocation.Deck));
			packet.writeInt16(mDuel.queryFieldCount(1, CardLocation.Extra));

			YGOGamePacket packet2 = new YGOGamePacket(packet);
			packet2.relaceByteAt(2, 1);

			YGOGamePacket packet3 = new YGOGamePacket(packet);
			if (mSwapped) {
				packet2.relaceByteAt(2, 0x11);
			} else {
				packet2.relaceByteAt(2, 0x10);
			}
			sendToTeam(packet, 0);
			sendToTeam(packet2, 1);
			sendToObservers(packet3);

			refershExtra(0, 0x81fff, true);
			refershExtra(1, 0x81fff, true);

			mDuel.start(opt);
			mTurnCount = 0;
			mLifePoints[0] = mConfig.startLp;
			mLifePoints[1] = mConfig.startLp;
			process();
		}
	}

	private void refershExtra(int player, int flag, boolean useCache) {
		byte[] result = mDuel.queryFieldCard(player, CardLocation.Extra, flag, useCache);
		YGOGamePacket update = new YGOGamePacket(GameMessage.UPDATE_DATA);
		update.writeByte(player);
		update.writeByte(CardLocation.Extra.intValue());
		update.write(result);
		mCurrentPlayers[player].send(update);
	}

	private void process() {
		int result = mDuel.process();
		if (result == -1) {
			mAttachedGameRoom.get().close();
		} else {
			endDuel(false);
		}
	}

	public void updateDeck(GameClient client, Packet packet) {
		Player boundedPlayer = client.getBoundedPlayer();
		synchronized (this) {
			int type = boundedPlayer.getType();
			if (type == PlayerType.Observer.intValue()) {
				return;
			}
			Deck deck = new Deck();
			int main = packet.readInt32();
			int side = packet.readInt32();
			for (int i = 0; i < main; i++) {
				deck.addMain(packet.readInt32());
			}
			for (int i = 0; i < side; i++) {
				deck.addSide(packet.readInt32());
			}
			if (mState.equals(GameState.Lobby)) {
				boundedPlayer.setDeck(deck);
				mIsReady[type] = false;
			} else if (mState.equals(GameState.Side)) {
				if (mIsReady[type]) {
					return;
				}
				if (!boundedPlayer.getDeck().check(deck)) {
					YGOGamePacket error = new YGOGamePacket(GameMessage.STOC_ERROR_MSG);
					error.writeByte(3);
					error.writeInt32(0);
					boundedPlayer.send(packet);
					return;
				}
				boundedPlayer.setDeck(deck);
				mIsReady[type] = true;
				serverMessage(boundedPlayer.getName() + "is ready.");
				boundedPlayer.send(new YGOGamePacket(GameMessage.STOC_DUEL_START));
				matchSide();
			}
		}
	}

	public void response(GameClient client, Packet packet) {
		Player boundedPlayer = client.getBoundedPlayer();
		synchronized (this) {
			if (mState.equals(GameState.Duel)) {
				return;
			}
			if (!boundedPlayer.getPlayerState().equals(PlayerState.Response)) {
				return;
			}
			byte[] resp = packet.readToEnd();
			if (resp.length > 64) {
				return;
			}
			boundedPlayer.setPlayerState(PlayerState.None);
			setResponse(resp);
		}
	}
	
	public void surrenderLocked(GameClient client, int i) {
		Player boundedPlayer = client.getBoundedPlayer();
		synchronized (this) {
			surrender(boundedPlayer, i, false);
		}
	}
	

	public void addPlayer(Player player) {
		synchronized (this) {
			int pos = getAvailablePlayerPos();
			if (!mState.equals(GameState.Lobby)) {
				player.setType(PlayerType.Observer.intValue());
				if (mState.equals(GameState.End)) {
					return;
				}
				sendJoinGame(player);
				player.sendTypeChange(mHostPlayer.equals(player));
				player.send(new YGOGamePacket(GameMessage.STOC_DUEL_START));
				mObservers.add(player);
				
				if (GameState.Duel.equals(mState)) {
					initNewSpector(player);
				}
				return;
			}
		}
	}
	
	private void initNewSpector(Player player) {
		int deck1 = mDuel.queryFieldCount(0, CardLocation.Deck);
		int deck2 = mDuel.queryFieldCount(1, CardLocation.Deck);
		
		int hand1 = mDuel.queryFieldCount(0, CardLocation.Hand);
		int hand2 = mDuel.queryFieldCount(1, CardLocation.Hand);
		
		YGOGamePacket packet = new YGOGamePacket(GameMessage.START);
		packet.writeByte(mSwapped ? 0x11 : 0x10);
		packet.writeByte(mLifePoints[0]);
		packet.writeByte(mLifePoints[1]);
		packet.writeInt16(deck1 + hand1);
		packet.writeInt16(mDuel.queryFieldCount(0, CardLocation.Extra));
		packet.writeInt16(deck2 + hand2);
		packet.writeInt16(mDuel.queryFieldCount(1, CardLocation.Extra));
		player.send(packet);
		
		YGOGamePacket draw = new YGOGamePacket(GameMessage.DRAW);
		draw.writeByte(0);
		draw.writeByte(hand1);
		
		for (int i = 0; i < hand1; i++) {
			draw.writeInt32(0);
		}
		player.send(draw);
		
		draw = new YGOGamePacket(GameMessage.NEW_TURN);
		draw.writeByte(1);
		draw.writeByte(hand2);
		for (int i = 0; i < hand2; i ++) {
			draw.writeByte(0);
		}
		player.send(draw);
		
		YGOGamePacket turn = new YGOGamePacket(GameMessage.NEW_TURN);
		turn.writeByte(0);
		player.send(turn);
		if (mCurrentPlayer == 1) {
			turn = new YGOGamePacket(GameMessage.NEW_TURN);
			turn.writeByte(0);
			player.send(turn);
		}
		initSpectatorLocation(player, CardLocation.MonsterZone);
		initSpectatorLocation(player, CardLocation.SpellZone);
		initSpectatorLocation(player, CardLocation.Grave);
		initSpectatorLocation(player, CardLocation.Removed);
	}

	private void initSpectatorLocation(Player player, CardLocation loc) {
		for (int index = 0; index < 2; index++) {
			int flag = loc == CardLocation.MonsterZone ? 0x91fff : 0x81fff;
			byte[]  result = mDuel.queryFieldCard(index, loc, flag, false);
			ByteBuffer buffer = ByteBuffer.wrap(result);
			while (buffer.hasRemaining()) {
				int len = buffer.getInt();
				if (len == 4) {
					continue;
				}
				int position = buffer.position();
				int endPos = position + len - 4;
				//TODO
			}
		}
	}

	private void sendJoinGame(Player player) {
		YGOGamePacket join = new YGOGamePacket(GameMessage.STOC_JOIN_GAME);
		join.writeInt32(mBanlist == null ? 0 : mBanlist.hashCode());
		join.writeByte(mConfig.rule);
		join.writeByte(mConfig.mode);
		join.writeByte(mConfig.enablePriority ? 1 : 0);
		join.writeByte(mConfig.enablePriority ? 1 : 0);
		for (int i = 0; i < 3; i ++) {
			join.writeByte(0);
		}
		join.writeInt32(mConfig.startLp);
		join.writeByte(mConfig.startHand);
		join.writeByte(mConfig.drawCount);
		join.writeInt16(mConfig.gameTimer);
		player.send(join);
		if (!mState.equals(GameState.Lobby)) {
			sendDuelingPlayers(player);
		}
	}

	private void sendDuelingPlayers(Player player) {
		for (int i = 0; i < mPlayers.length; i++) {
			YGOGamePacket enter = new YGOGamePacket(GameMessage.STOC_HS_PLAYER_ENTER);
			int id = i;
			if (mSwapped) {
				if (isTag) {
					if (i == 0 || id == 1) {
						id = i + 2;
					} else {
						id = i - 2;
					}
				} else {
					id = 1 - i;
				}
				try {
					enter.write(mPlayers[id].getName().getBytes("utf-8"), 0, 20);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					enter.write(mPlayers[id].getName().getBytes(), 0, 20);
				}
				enter.writeByte(i);
				player.send(enter);
			}
		}
	}

	private void setResponse(byte[] resp) {
		if (!mReplay.isDisabled()) {
			mReplay.writeByte(resp.length);
			mReplay.write(resp);
			mReplay.check();
		}
		mDuel.setResponse(resp);
		process();
	}

	private void matchSide() {
		if (mIsReady[0] && mIsReady[1]) {
			mState = GameState.Starting;
			isTpSelect = true;
			cancelSideTimer();
			setTpTimer();
			mPlayers[mStartPlayer].send(new YGOGamePacket(GameMessage.STOC_SELECT_TP));
		}
	}
	
	private void cancelSideTimer() {
		if (mChangeSideTasks == null) {
			return;
		}
		// cancel side timer
		for (TimerTask t : mChangeSideTasks) {
			if (t != null) {
				t.purge();
			}
		}
	}

	private void cancelTpTimer() {
		if (mTpTasks == null) {
			return;
		}
		// cancel side timer
		for (TimerTask t : mTpTasks) {
			if (t != null) {
				t.purge();
			}
		}
	}
	
	private void cancelHandTimer() {
		if (mTpTasks == null) {
			return;
		}
		// cancel side timer
		for (TimerTask t : mHandTasks) {
			if (t != null) {
				t.purge();
			}
		}
	}
	
	
	private void setHandTimer() {
		mHandTasks = new TimerTask[3];
		mHandTasks[0] = new EchoAlarmTimeTask(15);
		mHandTasks[1] = new EchoAlarmTimeTask(5, 4, 3, 2, 1);
		mHandTasks[2] = new HandEndTimeTask();
		GameManager.submit(mHandTasks[0], 15 * 1000);
		GameManager.submit(mHandTasks[1], 25 * 1000);
		GameManager.submit(mHandTasks[2], 30 * 1000);
	}

	private void setTpTimer() {
		mTpTasks = new TimerTask[3];
		mTpTasks[0] = new EchoAlarmTimeTask(15);
		mTpTasks[1] = new EchoAlarmTimeTask(5, 4, 3, 2, 1);
		mTpTasks[2] = new TpEndTimeTask();
		GameManager.submit(mTpTasks[0], 15 * 1000);
		GameManager.submit(mTpTasks[1], 25 * 1000);
		GameManager.submit(mTpTasks[2], 30 * 1000);
	}

	private void setSideTimer() {
		mChangeSideTasks = new TimerTask[4];
		mChangeSideTasks[0] = new EchoAlarmTimeTask(60);
		mChangeSideTasks[1] = new EchoAlarmTimeTask(30);
		mChangeSideTasks[2] = new EchoAlarmTimeTask(10);
		mChangeSideTasks[3] = new EchoAlarmTimeTask(5, 4, 3, 2, 1);
		mChangeSideTasks[4] = new SideEndTimeTask();
		GameManager.submit(mChangeSideTasks[0], 60 * 1000);
		GameManager.submit(mChangeSideTasks[1], 90 * 1000);
		GameManager.submit(mChangeSideTasks[2], 110 * 1000);
		GameManager.submit(mChangeSideTasks[3], 115 * 1000);
		GameManager.submit(mChangeSideTasks[4], 120 * 1000);
	}
}
