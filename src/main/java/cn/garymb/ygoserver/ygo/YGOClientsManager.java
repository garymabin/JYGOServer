package cn.garymb.ygoserver.ygo;

import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import cn.garymb.ygoserver.server.Packet;
import cn.garymb.ygoserver.server.IOProcessor;

public class YGOClientsManager extends YGOConnectionManager<YGOIOService<Object>> {

	private static final Logger log = Logger.getLogger(YGOClientsManager.class.getName());
	
	private IOProcessor[] processors;

	@Override
	protected YGOIOService<Object> getYGOIOService() {
		return new GameClient();
	}

	@Override
	public Queue<Packet> processSocketData(YGOIOService<Object> serv) {
		Packet p = null;
		String id = serv.getConncetionId();
		while ((p = serv.getReceivedPackets().poll()) != null) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Processing socket data: {0} from connection: {1}",
						new Object[] { p.toString(), id });
			}
		}
		// FIXME: this process is useless for now, cause we do not currently
		// have any message need to be handle besides clients
		return null;
	}

	@Override
	public void serviceStarted(YGOIOService<Object> service) {
		super.serviceStarted(service);
		String id = getUniqueId(service);
		String connectionId = getName() + id;
		service.setConnectionId(connectionId);
		service.setProcessors(processors);
	}

	@Override
	public void reconnectionFailed(Map<String, Object> port_props) {
	}

}
