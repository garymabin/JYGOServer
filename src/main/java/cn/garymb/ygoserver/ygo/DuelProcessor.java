package cn.garymb.ygoserver.ygo;

import java.io.IOException;

import cn.garymb.ygoserver.server.IOProcessor;
import cn.garymb.ygoserver.server.Packet;
import cn.garymb.ygoserver.ygo.ocgwrapper.type.GameMessage;

public class DuelProcessor implements IOProcessor {

	public String getId() {
		return getClass().getName();
	}

	public boolean processIncoming(YGOIOService<?> service, Packet packet) {
		int msgType = ((YGOGamePacket) packet).getType();
		switch (msgType) {
		case GameMessage.CTOS_CHAT:
			onChat((GameClient) service, packet);
			break;
		case GameMessage.CTOS_HS_TO_DUEL_LIST:
			onMoveToDuelList((GameClient) service, packet);
			break;
		case GameMessage.CTOS_HS_TO_OBSERVER:
			onMoveToObserver((GameClient) service, packet);
			break;
		case GameMessage.CTOS_LEAVE_GAME:
			onRemovePlayer((GameClient) service, packet);
			break;
		case GameMessage.CTOS_HS_READY:
		case GameMessage.CTOS_HS_NOT_READY:
		case GameMessage.CTOS_HS_KICK:
		case GameMessage.CTOS_HS_START:
		case GameMessage.CTOS_HAND_RESULT:
		case GameMessage.CTOS_TP_RESULT:
		case GameMessage.CTOS_UPDATE_DECK:
		case GameMessage.CTOS_RESPONSE:
		case GameMessage.CTOS_SURRENDER:
			break;

		default:
			break;
		}
		return false;
	}

	private void onRemovePlayer(GameClient client, Packet packet) {
		GameRoom room = client.getBoundedGameRoom();
		if (room != null) {
			room.removePlayer(client);
		}
	}

	private void onMoveToObserver(GameClient client, Packet packet) {
		GameRoom room = client.getBoundedGameRoom();
		if (room != null) {
			room.moveToObserver(client);
		}
	}

	private void onMoveToDuelList(GameClient client, Packet packet) {
		GameRoom room = client.getBoundedGameRoom();
		if (room != null) {
			room.moveToDuelList(client);
		}
	}

	private void onChat(GameClient client, Packet packet) {
		GameRoom room = client.getBoundedGameRoom();
		// FIXME: maybe we should do some more varification
		if (room != null) {
			room.chat(client, packet.read(256));
		}
	}

	public boolean processOutgoing(YGOIOService<?> service, Packet packet) {
		return false;
	}

	public void packetsSent(YGOIOService<?> service) throws IOException {
	}

	public void processCommand(YGOIOService<?> service, Packet packet) {
	}

	public boolean serviceStopped(YGOIOService<?> service, boolean streamClosed) {
		return false;
	}

}
