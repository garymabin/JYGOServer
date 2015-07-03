package cn.garymb.ygoserver.ygo;

import java.lang.ref.WeakReference;

import cn.garymb.ygoserver.server.Packet;
import cn.garymb.ygoserver.ygo.ocgwrapper.type.GameMessage;

public class Player {
	
	private WeakReference<GameClient> mAttachedClient;

	public Player(GameClient gameClient) {
		mAttachedClient = new WeakReference<GameClient>(gameClient);
	}
	
	public static void parse(Packet p) {
		if (p instanceof YGOGamePacket) {
			int ctos = ((YGOGamePacket) p).readCtos();
			switch (ctos) {
			case GameMessage.CTOS_PLAYER_INFO:
				
				break;

			default:
				break;
			}
		}
	}

}
