/**
 * ConnectionOpenListener.java
 * author: mabin
 * 2015年3月30日
 */
package cn.garymb.ygoserver.net;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public interface ConnectionOpenListener {
	public static final int DEF_RECEIVE_BUFFER_SIZE = 2 * 1024;
	
	public static final int IPTOS_LOWCOST = 0x02;
	
	public static final int IPTOS_LOWDELAY = 0x10;
	
	public static final int IPTOS_RELIABILITY = 0x04;
	
	public static final int IPTOS_THROUGHPUT = 0x08;
	
	public static final int DEF_TRAFFIC_CLASS = IPTOS_LOWCOST;
	
	void accept(SocketChannel sc);
	
	int getPort();
	
	String[] getIfcs();
	
	String getSRVType();
	
	String getRemoteHostname();
	
	InetSocketAddress getRemoteAddress();
	
	int getReceiveBufferSize();
	
	int getTrafficClass();
	
	ConnectionType getConnectionType();
}
