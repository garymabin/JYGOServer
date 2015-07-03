/**
 * ConnectionHandlerIfc.java
 * author: mabin
 * 2015年5月12日
 */
package cn.garymb.ygoserver.ygo;

import java.util.Queue;

import cn.garymb.ygoserver.server.Packet;

public interface ConnectionHandlerIfc<IO extends YGOIOService<?>> {
	boolean writePacketToSocket(IO serv, Packet packet);

	void writePacketsToSocket(IO serv, Queue<Packet> packets);

}
