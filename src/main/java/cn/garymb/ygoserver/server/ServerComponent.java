/**
 * ServerComponent.java
 * author: mabin
 * 2015年5月7日
 */
package cn.garymb.ygoserver.server;

import java.util.Queue;

public interface ServerComponent {
	
	void initializationCompleted();
	
	void processPacket(Packet packet, Queue<Packet> results);
	
	String getName();
	
	boolean isInitializationComplete();
	
	void setName(String name);
	
	void release();

}
