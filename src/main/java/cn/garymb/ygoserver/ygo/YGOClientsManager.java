package cn.garymb.ygoserver.ygo;

import java.util.Map;
import java.util.Queue;

import cn.garymb.ygoserver.server.Packet;

public class YGOClientsManager extends YGOConnectionManager<YGOIOService<Object>> {

	@Override
	protected YGOIOService<Object> getYGOIOService() {
		return new GameClient();
	}

	@Override
	public Queue<Packet> processSocketData(YGOIOService<Object> serv) {
		return null;
	}

	@Override
	public void reconnectionFailed(Map port_props) {
	}

}
