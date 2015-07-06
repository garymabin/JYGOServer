package cn.garymb.ygoserver.ygo;

import java.io.IOException;

import cn.garymb.ygoserver.server.IOProcessor;
import cn.garymb.ygoserver.server.Packet;

public class GameMessageProcessor implements IOProcessor {

	public String getId() {
		return getClass().getName();
	}

	public boolean processIncoming(YGOIOService service, Packet packet) {
		return false;
	}

	public boolean processOutgoing(YGOIOService service, Packet packet) {
		return false;
	}

	public void packetsSent(YGOIOService service) throws IOException {
	}

	public void processCommand(YGOIOService service, Packet packet) {
	}

	public boolean serviceStopped(YGOIOService service, boolean streamClosed) {
		return false;
	}
}
