package cn.garymb.ygoserver.ygo;

import java.lang.ref.WeakReference;

import cn.garymb.ygoserver.server.Packet;
import cn.garymb.ygoserver.ygo.ocgwrapper.type.GameMessage;
import cn.garymb.ygoserver.ygo.ocgwrapper.type.PlayerType;

public class Player {
	
	private WeakReference<GameClient> mAttachedClient;
	
	private String name;
	
	private Deck mDeck;
	
	private PlayerState mPlayerState;
	
	private int type;

	public Player(GameClient gameClient) {
		mAttachedClient = new WeakReference<GameClient>(gameClient);
		setPlayerState(PlayerState.None);
	}

	public String getName() {
		return name;
	}
	
	public GameClient getAttachedClient() {
		return mAttachedClient.get();
	}

	/*package*/ void setName(String name) {
		this.name = name;
	}

	public int getType() {
		return type;
	}

	/*package*/  void setType(int type) {
		this.type = type;
	}
	
	/*package*/ void send(Packet p) {
		GameClient c = mAttachedClient.get();
		if (c != null) {
			c.addPacketToSend(p);
		}
	}
 	
	/*package*/ void sendTypeChange(boolean isHostPlayer) {
		YGOGamePacket p = new YGOGamePacket(GameMessage.STOC_TYPE_CHANGE);
		p.writeByte((byte)(this.type + (isHostPlayer ? PlayerType.Host.intValue() : 0)));
		send(p);
	}

	public Deck getDeck() {
		return mDeck;
	}

	/*package*/ void setDeck(Deck mDeck) {
		this.mDeck = mDeck;
	}

	public PlayerState getPlayerState() {
		return mPlayerState;
	}

	/*package*/ void setPlayerState(PlayerState mPlayerState) {
		this.mPlayerState = mPlayerState;
	}
}
