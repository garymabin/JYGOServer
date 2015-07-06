/**
 * YGOIOProcessor.java
 * author: mabin
 * 2015年5月12日
 */
package cn.garymb.ygoserver.server;

import java.io.IOException;
import java.util.Map;

import cn.garymb.ygoserver.ygo.YGOConnectionManager;
import cn.garymb.ygoserver.ygo.YGOIOService;


public interface IOProcessor {
	/**
	 * Returns identifier of processor
	 * 
	 * 
	 */
	String getId();
	
	
	/**
	 * Process packets read from socket as they are sent to SessionManager.
	 * 
	 * @param service
	 * @param packet
	 * @return true if packet should not be forwarded
	 * @throws IOException 
	 */
	boolean processIncoming(YGOIOService service, Packet packet);
	
	/**
	 * Process outgoing packets as they are added to XMPPIOService outgoing 
	 * packets queue.
	 * 
	 * @param service
	 * @param packet
	 * @return true if packet should be removed
	 * @throws IOException 
	 */
	boolean processOutgoing(YGOIOService service, Packet packet);

	/**
	 * Method is called when all waiting data was written to socket.
	 * 
	 * @param service
	 * @throws IOException 
	 */
	void packetsSent(YGOIOService service) throws IOException;
	
	/**
	 * Process command execution which may be sent from other component and 
	 * should be processed by processor
	 * 
	 * @param packet 
	 */
	void processCommand(YGOIOService service, Packet packet);
	
	/**
	 * Method called when XMPPIOService is closed.
	 * 
	 * @param service 
	 * @param streamClosed 
	 * @return true if connecton manager should not be notified about stopping 
	 *				of this service
	 */
	boolean serviceStopped(YGOIOService service, boolean streamClosed);
	
//	/**
//	 * Sets connection manager instance for which this XMPPIOProcessor is used
//	 * 
//	 * @param connectionManager 
//	 */
//	void setConnectionManager(YGOConnectionManager connectionManager);
//	
//	/**
//	 * Method used for setting properties
//	 * 
//	 * @param props 
//	 */
//	void setProperties(Map<String,Object> props);
}
