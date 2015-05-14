/**
 * ConnectionHandlerIfc.java
 * author: mabin
 * 2015年5月12日
 */
package cn.garymb.ygoserver.server.ygoserver;

import java.util.Queue;

import cn.garymb.ygoserver.server.Packet;
import cn.garymb.ygoserver.ygo.YGOIOService;

public interface ConnectionHandlerIfc<IO extends YGOIOService<?>> {
	boolean writePacketToSocket(IO serv, Packet packet);

	void writePacketsToSocket(IO serv, Queue<Packet> packets);

}
