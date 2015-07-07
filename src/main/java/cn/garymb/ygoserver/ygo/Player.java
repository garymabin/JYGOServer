package cn.garymb.ygoserver.ygo;

import java.lang.ref.WeakReference;

import cn.garymb.ygoserver.ygo.ocgwrapper.type.GameMessage;
import cn.garymb.ygoserver.ygo.ocgwrapper.type.PlayerType;

public class Player {
	
	private WeakReference<GameClient> mAttachedClient;
	
	private String name;
	
	private int type;

	public Player(GameClient gameClient) {
		mAttachedClient = new WeakReference<GameClient>(gameClient);
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
	
	/*package*/ void sendTypeChange(boolean isHostPlayer) {
		GameClient c = mAttachedClient.get();
		if (c != null) {
			YGOGamePacket p = new YGOGamePacket(GameMessage.STOC_TYPE_CHANGE);
			p.writeByte((byte)(this.type + (isHostPlayer ? PlayerType.Host.intValue() : 0)));
			c.addPacketToSend(p);
		}
	}
}
