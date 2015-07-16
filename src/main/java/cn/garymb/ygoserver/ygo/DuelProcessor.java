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
			onReadyChange((GameClient) service, packet, true);
		case GameMessage.CTOS_HS_NOT_READY:
			onReadyChange((GameClient) service, packet, false);
		case GameMessage.CTOS_HS_KICK:
			onKick((GameClient) service, packet);
		case GameMessage.CTOS_HS_START:
			onStartDuel((GameClient) service, packet);
		case GameMessage.CTOS_HAND_RESULT:
			onHandResult((GameClient) service, packet);
		case GameMessage.CTOS_TP_RESULT:
			onTpResult((GameClient) service, packet);
		case GameMessage.CTOS_UPDATE_DECK:
		case GameMessage.CTOS_RESPONSE:
		case GameMessage.CTOS_SURRENDER:
			break;

		default:
			break;
		}
		return false;
	}

	private void onTpResult(GameClient client, Packet packet) {
		boolean tp = packet.readByte() != 0;
		GameRoom room = client.getBoundedGameRoom();
		if (room != null) {
			room.tpResult(client, tp);
		}
	}

	private void onHandResult(GameClient client, Packet packet) {
		int res = packet.readByte();
		GameRoom room = client.getBoundedGameRoom();
		if (room != null) {
			room.handResult(client, res);
		}
	}

	private void onStartDuel(GameClient client, Packet packet) {
		GameRoom room = client.getBoundedGameRoom();
		if (room != null) {
			room.startDuel(client);
		}
	}

	private void onKick(GameClient client, Packet packet) {
		GameRoom room = client.getBoundedGameRoom();
		int pos = packet.readByte();
		if (room != null) {
			room.kick(client, pos);
		}
	}

	private void onReadyChange(GameClient client, Packet packet, boolean isReady) {
		GameRoom room = client.getBoundedGameRoom();
		if (room != null) {
			room.setReady(client, isReady);
		}
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
