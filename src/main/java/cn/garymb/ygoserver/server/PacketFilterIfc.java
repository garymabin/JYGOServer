/**
 * PacketFilterIfc.java
 * author: mabin
 * 2015年5月7日
 */
package cn.garymb.ygoserver.server;

import cn.garymb.ygoserver.stats.StatisticList;


public interface PacketFilterIfc {
	
	void init(String name, QueueType qType);
	
	Packet filter(Packet packet);
	
	void getStatistics(StatisticList list);

}
