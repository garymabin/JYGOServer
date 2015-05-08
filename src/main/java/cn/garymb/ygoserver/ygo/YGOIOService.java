/**
 * YGOIOService.java
 * author: mabin
 * 2015年4月2日
 */
package cn.garymb.ygoserver.ygo;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import cn.garymb.ygoserver.net.IOService;
import cn.garymb.ygoserver.net.IOServiceListener;
import cn.garymb.ygoserver.server.Packet;

public class YGOIOService<RefObject> extends IOService<RefObject> {
	
	private long                  packetsReceived      = 0;
	private long                  packetsSent          = 0;
	
	private long                  totalPacketsReceived = 0;
	private long                  totalPacketsSent     = 0;
	
	private ConcurrentLinkedQueue<Packet> receivedPackets =
			new ConcurrentLinkedQueue<Packet>();
	@SuppressWarnings("rawtypes")
	private YGOIOServiceListener serviceListener;

	@Override
	protected void processSocketData() throws IOException {
		
	}

	@Override
	protected int receivedPackets() {
		return 0;
	}
	
	public long getPacketsReceived(boolean reset) {
		long tmp = packetsReceived;

		if (reset) {
			packetsReceived = 0;
		}

		return tmp;
	}
	
	public long getPacketsSent(boolean reset) {
		long tmp = packetsSent;

		if (reset) {
			packetsSent = 0;
		}

		return tmp;
	}
	
	public long getTotalPacketsReceived() {
		return totalPacketsReceived;
	}
	
	public long getTotalPacketsSent() {
		return totalPacketsSent;
	}
	
	public Queue<Packet> getReceivedPackets() {
		return receivedPackets;
	}
	
	@SuppressWarnings("rawtypes")
	public void setIOServiceListener(YGOIOServiceListener sl) {
		this.serviceListener = sl;
		super.setIOServiceListener(sl);
	}

}
