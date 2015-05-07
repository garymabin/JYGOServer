/**
 * ReceiverTimeoutHandler.java
 * author: mabin
 * 2015年5月7日
 */
package cn.garymb.ygoserver.server;

public interface ReceiverTimeoutHandler {
	
	void timeOutExpired(Packet data);

	void responseReceived(Packet data, Packet response);

}
