/**
 * MessageReceiver.java
 * author: mabin
 * 2015年5月7日
 */
package cn.garymb.ygoserver.server;

import java.util.Queue;


public interface MessageReceiver extends ServerComponent {
	
	boolean addPacket(Packet packet);

	boolean addPacketNB(Packet packet);
	
	boolean addPackets(Queue<Packet> packets);
	
	void start();

}
