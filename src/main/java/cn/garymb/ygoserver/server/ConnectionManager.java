/**
 * ConnectionManager.java
 * author: mabin
 * 2015年4月2日
 */
package cn.garymb.ygoserver.server;

import java.util.logging.Logger;

import cn.garymb.ygoserver.net.ConnectionOpenThread;
import cn.garymb.ygoserver.ygo.YGOIOService;

public class ConnectionManager<IO extends YGOIOService<?>> extends AbstractMessageReceiver {
	
	private static final Logger         log = Logger.getLogger(ConnectionManager.class
			.getName());
	
	private static ConnectionOpenThread connectThread = ConnectionOpenThread.getInstance();

	@Override
	public void processPacket(Packet packet) {
		// TODO Auto-generated method stub
		
	}

}
