package cn.garymb.ygoserver.ygo;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

import cn.garymb.ygoserver.server.IOProcessor;
import cn.garymb.ygoserver.server.Packet;
import cn.garymb.ygoserver.server.YGOCoreMain;
import cn.garymb.ygoserver.util.TextUtils;
import cn.garymb.ygoserver.ygo.ocgwrapper.type.GameMessage;
import cn.garymb.ygoserver.ygo.ocgwrapper.type.PlayerType;

public class GameRoomProcessor implements IOProcessor {

	private static final Logger log = Logger.getLogger(GameRoomProcessor.class.getName());

	public String getId() {
		return getClass().getName();
	}

	public boolean processIncoming(YGOIOService<?> service, Packet packet) {
		int msgType = ((YGOGamePacket)packet).getType();
		switch (msgType) {
		case GameMessage.CTOS_PLAYER_INFO:
			onPlayerInfo((GameClient)service,(YGOGamePacket) packet);
			break;
		case GameMessage.CTOS_JOIN_GAME:
			onJoinMycardStyleGame((GameClient)service,(YGOGamePacket) packet);
			break;
		case GameMessage.CTOS_CREATE_GAME:
			onCreateGame((GameClient)service,(YGOGamePacket) packet);
			break;
		default:
			break;
		}
		return false;
	}

	private void onCreateGame(GameClient client, YGOGamePacket packet) {
		Player p = client.getBoundedPlayer();
		if (TextUtils.isEmpty(p.getName()) || !(p.getType() == PlayerType.Undefined.intValue())) {
			return;
		}
		GameRoom room = null;
		room = GameManager.createOrGetGame(new MyCardStyleGameConfig(packet));
		if (room == null) {
			log.log(Level.SEVERE, "server full");
			return;
		}
		if (client.setBoundedGameRoom(room) ) {
			room.addClient(client);	
		}
	}

	private void onJoinMycardStyleGame(GameClient client, YGOGamePacket packet) {
		Player p = client.getBoundedPlayer();
		if (TextUtils.isEmpty(p.getName()) || !(p.getType() == PlayerType.Undefined.intValue())) {
			return;
		}
		int version = packet.readInt16();
		int requiredVersion = Integer.parseInt(YGOCoreMain.getConfigurator().getClientVersion(), 16);
		if (version < requiredVersion) {
			log.log(Level.WARNING, "required version higher than {0}", new Object[]{YGOCoreMain.getConfigurator().getClientVersion()});
			return;
		} else if (version > requiredVersion){
			log.log(Level.WARNING, "client version is higher than current version");
		}
		packet.readInt32();
		packet.readInt16();
		String joinCommand = null;
		try {
			joinCommand = new String(packet.read(60), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		if (!TextUtils.isEmpty(joinCommand)) {
			GameRoom room = null;
			GameConfig config = new MyCardStyleGameConfig(joinCommand);
			if (GameManager.isGameExist(config)) {
				room = GameManager.getRoom(config.name);
			} else {
				room = GameManager.createOrGetGame(config);
			}
			if (room == null) {
				log.log(Level.SEVERE, "server full");
				return;
			}
			if (!room.isOpen) {
				log.log(Level.WARNING, "game already finished");
				return;
			}
			if (client.setBoundedGameRoom(room)) {
				room.addClient(client);
			}
		}
	}

	private void onPlayerInfo(GameClient client, YGOGamePacket packet) {
		Player p = client.getBoundedPlayer();
		try {
			p.setName(new String(packet.read(20), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			log.log(Level.WARNING, "invalid username");
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
