/**
 * IOService.java
 * author: mabin
 * 2015年3月31日
 */
package cn.garymb.ygoserver.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import cn.garymb.ygoserver.io.IOInterface;
import cn.garymb.ygoserver.io.SocketIO;

public class IOService<RefObject> implements Callable<IOService<?>> {
	
	private ByteBuffer                    socketInput     = null;
	private int                           socketInputSize = 2048;
	private IOInterface                   socketIO        = null;
	private boolean                       stopping        = false;
	private long[]                        wrData          = new long[60];
	
	private static final Logger log = Logger.getLogger(IOService.class.getName());
	
	protected CharBuffer        cb              = CharBuffer.allocate(2048);
	private final ReentrantLock writeInProgress = new ReentrantLock();
	private final ReentrantLock readInProgress  = new ReentrantLock();

	public void accept(final SocketChannel socketChannel) throws IOException {
		try {
			if (socketChannel.isConnectionPending()) {
				socketChannel.finishConnect();
			}
			socketIO = new SocketIO(socketChannel);
		} catch (IOException e) {
			
		}
	}

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
						", Exception while stopping service: " + connectionId, e);
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

	protected void writeData(Object object) {

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
			if ((data != null) && (data.length() > 0)) {
				if (log.isLoggable(Level.FINEST)) {
					if (data.length() < 256) {
						log.log(Level.FINEST, "Socket: {0}, Writing data ({1}): {2}", new Object[] {
								socketIO,
								data.length(), data });
					} else {
						log.log(Level.FINEST, "Socket: {0}, Writing data: {1}", new Object[] {
								socketIO,
								data.length() });
					}
				}

				ByteBuffer dataBuffer = null;

				// int out_buff_size = data.length();
				// int idx_start = 0;
				// int idx_offset = Math.min(idx_start + out_buff_size, data.length());
				//
				// while (idx_start < data.length()) {
				// String data_str = data.substring(idx_start, idx_offset);
				// if (log.isLoggable(Level.FINEST)) {
				// log.finest("Writing data_str (" + data_str.length() + "), idx_start="
				// + idx_start + ", idx_offset=" + idx_offset + ": " + data_str);
				// }
				encoder.reset();

				// dataBuffer = encoder.encode(CharBuffer.wrap(data, idx_start,
				// idx_offset));
				dataBuffer = encoder.encode(CharBuffer.wrap(data));
				encoder.flush(dataBuffer);
				socketIO.write(dataBuffer);
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Socket: {0}, wrote: {1}", new Object[] { socketIO,
							data.length() });
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
				log.log(Level.FINER, "Data writing exception " + connectionId, e);
			}
			forceStop();
		} finally {
			writeInProgress.unlock();
		}		
	}

}
