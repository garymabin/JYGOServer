/**
 * IOService.java
 * author: mabin
 * 2015年3月31日
 */
package cn.garymb.ygoserver.net;

import java.io.IOException;
import java.net.Socket;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import cn.garymb.ygoserver.io.IOInterface;
import cn.garymb.ygoserver.io.SocketIO;
import cn.garymb.ygoserver.stats.StatisticList;

public abstract class IOService<RefObject> implements Callable<IOService<?>> {
	
	private ByteBuffer                    socketInput     = null;
	private long                          lastTransferTime = 0;
	private int                           socketInputSize = 2048;
	private IOInterface                   socketIO        = null;
	private boolean                       stopping        = false;
	private long[]                        wrData          = new long[60];
	private long[]                        rdData          = new long[60];
	
	private static final Logger log = Logger.getLogger(IOService.class.getName());
	private static final long MAX_ALLOWED_EMPTY_CALLS = 100;
	
	protected ByteBuffer        bb              = ByteBuffer.allocate(2048);
	private final ReentrantLock writeInProgress = new ReentrantLock();
	private final ReentrantLock readInProgress  = new ReentrantLock();
	private long   empty_read_call_count = 0;
	private ConcurrentMap<String, Object> sessionData = new ConcurrentHashMap<String,
			Object>(4, 0.75f, 4);
	public static final String PORT_TYPE_PROP_KEY = "type";
	
	private IOServiceListener<IOService<RefObject>> serviceListener  = null;
	private ConnectionType connectionType;
	private RefObject refObject;
	private String local_address;
	private String remote_address;
	private String id;

	public void accept(final SocketChannel socketChannel) throws IOException {
		try {
			if (socketChannel.isConnectionPending()) {
				socketChannel.finishConnect();
			}
			socketIO = new SocketIO(socketChannel);
		} catch (IOException e) {
			String host = (String) sessionData.get("remote-hostname");

			if (host == null) {
				host = (String) sessionData.get("remote-host");
			}

			String sock_str = null;

			try {
				sock_str = socketChannel.socket().toString();
			} catch (Exception ex) {
				sock_str = ex.toString();
			}
			log.log(Level.FINER,
					"Problem connecting to remote host: {0}, address: {1}, socket: {2} - exception: {3}, session data: {4}",
					new Object[] { host,
					remote_address, sock_str, e, sessionData });

			throw e;

			
		}
		socketInputSize = socketIO.getSocketChannel().socket().getReceiveBufferSize();
		socketInput     = ByteBuffer.allocate(socketInputSize);
		socketInput.order(byteOrder());

		Socket sock = socketIO.getSocketChannel().socket();

		local_address  = sock.getLocalAddress().getHostAddress();
		remote_address = sock.getInetAddress().getHostAddress();
		id = local_address + "_" + sock.getLocalPort() + "_" + remote_address + "_" + sock
				.getPort();
		setLastTransferTime();
	}
	
	protected abstract void processSocketData() throws IOException;
	
	protected abstract int receivedPackets();

	/* (non-Javadoc)
	 * @see java.util.concurrent.Callable#call()
	 */
	public IOService<?> call() throws Exception {
		writeData(null);

		boolean readLock = true;

		if (stopping) {
			stop();
		} else {
			readLock = readInProgress.tryLock();
			if (readLock) {
				try {
					processSocketData();
					if ((receivedPackets() > 0) && (serviceListener != null)) {
						serviceListener.packetsReady(this);
					}    // end of if (receivedPackets.size() > 0)
				} finally {
					readInProgress.unlock();
				}
			}
		}

		return readLock
				? this
				: null;
	}
	
	/**
	 * Describe
	 * <code>stop</code> method here.
	 *
	 */
	public void stop() {
		if ((socketIO != null) && socketIO.waitingToSend()) {
			stopping = true;
		} else {
			forceStop();
		}
	}
	
	public void forceStop() {
		if (log.isLoggable(Level.FINER)) {
			log.log(Level.FINER, "Socket: {0}, Force stop called...", socketIO);
		}
		try {
			if ((socketIO != null) && socketIO.isConnected()) {
				synchronized (socketIO) {
					if (log.isLoggable(Level.FINER)) {
						log.log(Level.FINER, "Calling stop on: {0}", socketIO);
					}
					socketIO.stop();
				}
			}
		} catch (Exception e) {

			// Well, do nothing, we are closing the connection anyway....
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Socket: " + socketIO +
						", Exception while stopping service: ", e);
			}
		} finally {
			if (serviceListener != null) {
				if (log.isLoggable(Level.FINER)) {
					log.log(Level.FINER, "Calling stop on the listener: {0}", serviceListener);
				}

				IOServiceListener<IOService<RefObject>> tmp = serviceListener;

				serviceListener = null;

				// The temp can still be null if the forceStop is called concurrently
				if (tmp != null) {
					tmp.serviceStopped(this);
				}
			} else {
				if (log.isLoggable(Level.FINER)) {
					log.log(Level.FINER, "Service listener is null: {0}", socketIO);
				}
			}
		}
	}
	
	@Override
	public String toString() {
		return "type: " + connectionType + ", Socket: " + socketIO;
	}
	
	public ConnectionType connectionType() {
		return this.connectionType;
	}
	
	public boolean waitingToRead() {
		return true;
	}
	
	public boolean waitingToSend() {
		return socketIO.waitingToSend();
	}
	
	public int waitingToSendSize() {
		return socketIO.waitingToSendSize();
	}
	
	public long getBuffOverflow(boolean reset) {
		return socketIO.getBuffOverflow(reset);
	}
	
	public long getBytesReceived(boolean reset) {
		return socketIO.getBytesReceived(reset);
	}
	
	public long getBytesSent(boolean reset) {
		return socketIO.getBytesSent(reset);
	}
	
	public String getLocalAddress() {
		return local_address;
	}
	
	public long[] getReadCounters() {
		return rdData;
	}
	
	public String getUniqueId() {
		return id;
	}
	
	public RefObject getRefObject() {
		return refObject;
	}
	
	public String getRemoteAddress() {
		return remote_address;
	}
	
	public long[] getWriteCounters() {
		return wrData;
	}
	
	public boolean isConnected() {
		boolean result = (socketIO == null)
				? false
				: socketIO.isConnected();

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Socket: {0}, Connected: {1}", new Object[] {
					socketIO,
					result});
		}

		return result;
	}
	
	public SocketChannel getSocketChannel() {
		return socketIO.getSocketChannel();
	}
	
	public void getStatistics(StatisticList list, boolean reset) {
		if (socketIO != null) {
			socketIO.getStatistics(list, reset);
		}
	}
	
	public long getTotalBuffOverflow() {
		return socketIO.getTotalBuffOverflow();
	}
	
	public long getTotalBytesReceived() {
		return socketIO.getTotalBytesReceived();
	}
	
	public long getTotalBytesSent() {
		return socketIO.getTotalBytesSent();
	}
	
	public void setIOServiceListener(IOServiceListener<IOService<RefObject>> sl) {
		this.serviceListener = sl;
	}
	
	public void setRefObject(RefObject refObject) {
		this.refObject = refObject;
	}
	
	public long getLastTransferTime() {
		return lastTransferTime;
	}
	
	public ConcurrentMap<String, Object> getSessionData() {
		return sessionData;
	}
	
	public void setSessionData(Map<String, Object> props) {

		// Sometimes, some values are null which is allowed in the original Map
		// however, ConcurrentHashMap does not allow nulls as value so we have
		// to copy Maps carefully.
		sessionData = new ConcurrentHashMap<String, Object>(props.size());
		for (Map.Entry<String, Object> entry : props.entrySet()) {
			if (entry.getValue() != null) {
				sessionData.put(entry.getKey(), entry.getValue());
			}
		}
		connectionType = ConnectionType.valueOf(sessionData.get(PORT_TYPE_PROP_KEY)
				.toString());
	}
	
	protected boolean isInputBufferEmpty() {
		return (socketInput != null) && (socketInput.remaining() == socketInput.capacity());
	}
	
	protected ByteOrder byteOrder() {
		return ByteOrder.BIG_ENDIAN;
	}
	
	protected ByteBuffer readBytes() throws IOException {
		setLastTransferTime();
		if (log.isLoggable(Level.FINEST) && (empty_read_call_count > 10)) {
			Throwable thr = new Throwable();

			thr.fillInStackTrace();
			log.log(Level.FINEST, "Socket: " + socketIO, thr);
		}
		try {
			ByteBuffer tmpBuffer = socketIO.read(socketInput);

			if (socketIO.bytesRead() > 0) {
				empty_read_call_count = 0;

				return tmpBuffer;
			} else {
				if ((++empty_read_call_count) > MAX_ALLOWED_EMPTY_CALLS && (!writeInProgress
						.isLocked())) {
					log.log(Level.WARNING,
							"Socket: {0}, Max allowed empty calls excceeded, closing connection.",
							socketIO);
					forceStop();
				}
			}
		} catch (BufferUnderflowException ex) {
			resizeInputBuffer();
			return readBytes();
		} catch (Exception eof) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Socket: " + socketIO + ", Exception reading data", eof);
			}

			// eof.printStackTrace();
			forceStop();
		}

		return null;
	}

	private void resizeInputBuffer() throws IOException{
		int netSize = socketIO.getInputPacketSize();

		// Resize buffer if needed.
		// if (netSize > socketInput.remaining()) {
		if (netSize > socketInput.capacity() - socketInput.remaining()) {

			// int newSize = netSize + socketInput.capacity();
			int newSize = socketInput.capacity() + socketInputSize;

			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Socket: {0}, Resizing socketInput to {1} bytes.",
						new Object[] { socketIO,
						newSize });
			}

			ByteBuffer b = ByteBuffer.allocate(newSize);

			b.order(byteOrder());
			b.put(socketInput);
			socketInput = b;
		} else {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Socket: {0}, Compacting socketInput.", socketIO);
			}
			socketInput.compact();
		}		
	}

	protected void writeData(byte[] data) {

		// Try to lock the data writing method
		boolean locked = writeInProgress.tryLock();

		// If cannot lock and nothing to send, just leave
		if (!locked && (data == null)) {
			return;
		}

		// Otherwise wait.....
		if (!locked) {
			writeInProgress.lock();
		}

		// Avoid concurrent calls here (one from call() and another from
		// application)
		try {
			if ((data != null) && (data.length > 0)) {
				if (log.isLoggable(Level.FINEST)) {
					if (data.length < 256) {
						log.log(Level.FINEST, "Socket: {0}, Writing data ({1}): {2}", new Object[] {
								socketIO,
								data.length, data });
					} else {
						log.log(Level.FINEST, "Socket: {0}, Writing data: {1}", new Object[] {
								socketIO,
								data.length });
					}
				}

				ByteBuffer dataBuffer = ByteBuffer.wrap(data);
				socketIO.write(dataBuffer);
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Socket: {0}, wrote: {1}", new Object[] { socketIO,
							data.length });
				}
				// idx_start = idx_offset;
				// idx_offset = Math.min(idx_start + out_buff_size, data.length());
				// }
				setLastTransferTime();

				// addWritten(data.length());
				empty_read_call_count = 0;
			} else {
				if (socketIO.waitingToSend()) {
					socketIO.write(null);
					setLastTransferTime();
					empty_read_call_count = 0;
				}
			}
		} catch (Exception e) {
			if (log.isLoggable(Level.FINER)) {
				log.log(Level.FINER, "Data writing exception ", e);
			}
			forceStop();
		} finally {
			writeInProgress.unlock();
		}		
	}
	
	private void setLastTransferTime() {
		lastTransferTime = System.currentTimeMillis();
	}

}
