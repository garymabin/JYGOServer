/**
 * YGOIOService.java
 * author: mabin
 * 2015年4月2日
 */
package cn.garymb.ygoserver.ygo;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import cn.garymb.ygoserver.net.IOService;
import cn.garymb.ygoserver.server.Packet;
import cn.garymb.ygoserver.server.YGOIOProcessor;

public class YGOIOService<RefObject> extends IOService<RefObject> {
	
	private static final Logger log = Logger.getLogger(YGOIOService.class.getName());
	
	private long                  packetsReceived      = 0;
	private long                  packetsSent          = 0;
	
	private long                  totalPacketsReceived = 0;
	private long                  totalPacketsSent     = 0;
	
	private YGOIOProcessor[]     processors           = null;
	
	private ConcurrentLinkedQueue<Packet> waitingPackets =
			new ConcurrentLinkedQueue<Packet>();
	
	private ConcurrentLinkedQueue<Packet> receivedPackets =
			new ConcurrentLinkedQueue<Packet>();
	@SuppressWarnings("rawtypes")
	private YGOIOServiceListener serviceListener;
	
	public ReentrantLock writeInProgress = new ReentrantLock();

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
	
	public void setProcessors(YGOIOProcessor[] processors) {
		this.processors = processors;
	}

	
	public void addPacketToSend(Packet packet) {

		// processing packet using io level processors
		if (processors != null) {
			for (YGOIOProcessor processor : processors) {
				if (processor.processOutgoing(this, packet)) {
					return;
				}
			}
		}
		++packetsSent;
		++totalPacketsSent;
		waitingPackets.offer(packet);
	}

	@Override
	public void processWaitingPackets() throws IOException {
		Packet packet = null;

		// int cnt = 0;
		// while ((packet = waitingPackets.poll()) != null && (cnt < 1000)) {
		while ((packet = waitingPackets.poll()) != null) {

			// ++cnt;
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "{0}, Sending packet: {1}", new Object[] { toString(),
						packet });
			}
			writeRawData(packet.getBytes());
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "{0}, SENT: {1}", new Object[] { toString(),
						packet.toString() });
			}
		}    // end of while (packet = waitingPackets.poll() != null)

		// notify io processors that all waiting packets were sent
		if (processors != null) {
			for (YGOIOProcessor processor : processors) {
				processor.packetsSent(this);
			}
		}		
	}
	
	public void writeRawData(byte[] data) throws IOException {

		// We change state of this object in this method
		// It can be called by many threads simultanously
		// so we need to make it thread-safe
		// writeLock.lock();
		// try {
		writeData(data);

		// } finally {
		// writeLock.unlock();
		// }
	}
	

}
