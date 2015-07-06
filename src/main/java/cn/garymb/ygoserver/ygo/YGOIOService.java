/**
 * YGOIOService.java
 * author: mabin
 * 2015年4月2日
 */
package cn.garymb.ygoserver.ygo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import cn.garymb.ygoserver.net.IOService;
import cn.garymb.ygoserver.server.Packet;
import cn.garymb.ygoserver.server.IOProcessor;

public class YGOIOService<RefObject> extends IOService<RefObject> {

	private static final Logger log = Logger.getLogger(YGOIOService.class.getName());

	private long packetsReceived = 0;
	private long packetsSent = 0;

	private long totalPacketsReceived = 0;
	private long totalPacketsSent = 0;

	private IOProcessor[] processors = null;

	private ConcurrentLinkedQueue<Packet> waitingPackets = new ConcurrentLinkedQueue<Packet>();

	private ConcurrentLinkedQueue<Packet> receivedPackets = new ConcurrentLinkedQueue<Packet>();
	@SuppressWarnings("rawtypes")
	private YGOIOServiceListener serviceListener;

	public ReentrantLock writeInProgress = new ReentrantLock();

	@Override
	protected void processSocketData() throws IOException {
		if (isConnected()) {
			ByteBuffer data = readBytes();
			//FIXME: try to do some data check;
			if (isConnected() && (data != null) && (data.position() > 0)) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "{0}, READ:{1}", new Object[] { toString(), data });
				}
				boolean disconnect = checkData(data);
				if (disconnect) {
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE, "{0}, checkData says disconnect: {1}", new Object[] { toString(), data });
					} else {
						log.log(Level.WARNING, "{0}, checkData says disconnect", toString());
					}
					forceStop();
				}
				YGOGamePacket p = new YGOGamePacket(data.array());
				addReceivedPacket(p);
			}
		} else {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE,
						"{0}, function called when the service is not connected! forceStop()",
						toString());
			}
			forceStop();
		}
	}

	protected void addReceivedPacket(final Packet packet) {
		if (processors != null) {
			boolean stop = false;

			for (IOProcessor processor : processors) {
				stop |= processor.processIncoming(this, packet);
			}
			if (stop) {
				return;
			}
		}
		++packetsReceived;
		++totalPacketsReceived;
		receivedPackets.offer(packet);
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

	public void setProcessors(IOProcessor[] processors) {
		this.processors = processors;
	}

	public void addPacketToSend(Packet packet) {

		// processing packet using io level processors
		if (processors != null) {
			for (IOProcessor processor : processors) {
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
				log.log(Level.FINEST, "{0}, Sending packet: {1}", new Object[] { toString(), packet });
			}
			writeRawData(packet.getBytes());
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "{0}, SENT: {1}", new Object[] { toString(), packet.toString() });
			}
		} // end of while (packet = waitingPackets.poll() != null)

		// notify io processors that all waiting packets were sent
		if (processors != null) {
			for (IOProcessor processor : processors) {
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

	/**
	 *
	 * @param data
	 *
	 *
	 * @return a value of <code>boolean</code>
	 * @throws IOException
	 */
	public boolean checkData(ByteBuffer data) throws IOException {

		// by default do nothing and return false
		return false;
	}
	
	@Override
	public void forceStop() {
		boolean stop = false;
		if (processors != null) {
			for (IOProcessor processor : processors) {
				stop |= processor.serviceStopped(this, false);
			}
		}
		if (!stop) {
			super.forceStop();
		}
	}

}
