package cn.garymb.ygoserver.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import cn.garymb.ygoserver.net.ConnectionOpenListener;
import cn.garymb.ygoserver.stats.StatisticList;

public class SocketIO implements IOInterface {
	
	private static final Logger log  = Logger.getLogger(SocketIO.class.getName());
	
	private static final int MAX_USER_IO_QUEUE_SIZE_PROP_DEF = 1000;
	
	private static final String MAX_USER_IO_QUEUE_SIZE_PROP_KEY = "max-user-io-queue-size";
	
	private long bufferOverflow = 0;
	private int bytesRead = 0;
	private long bytesReceived = 0;
	private SocketChannel channel = null;
	private Queue<ByteBuffer> dataToSend = null;
	private String logId = null;
	private String remoteAddress =null;
	private long totalBuffOverflow = 0;
	private long totalBytesReceived = 0;
	private long totalBytesSent = 0;
	
	public SocketIO(final SocketChannel sock) throws IOException{
		channel = sock;
		channel.configureBlocking(false);
		channel.socket().setSoLinger(false, 0);
		channel.socket().setReuseAddress(true);
		channel.socket().setKeepAlive(true);
		remoteAddress = channel.socket().getInetAddress().getHostAddress();
		if (channel.socket().getTrafficClass() == ConnectionOpenListener.IPTOS_THROUGHPUT) {
			dataToSend = new LinkedBlockingQueue<ByteBuffer>(100000);
		} else {
			int queue_size = Integer.getInteger(MAX_USER_IO_QUEUE_SIZE_PROP_KEY, MAX_USER_IO_QUEUE_SIZE_PROP_DEF);
			dataToSend = new LinkedBlockingQueue<ByteBuffer>(queue_size);
		}
	}

	public int bytesRead() {
		return bytesRead;
	}

	public boolean checkCapabilities(String caps) {
		return false;
	}

	public int getInputPacketSize() throws IOException {
		return channel.socket().getReceiveBufferSize();
	}

	public SocketChannel getSocketChannel() {
		return channel;
	}
	
	public void getStatistics(StatisticList list, boolean reset) {
		
	}

	public long getBytesSent(boolean reset) {
		return 0;
	}

	public long getTotalBytesSent() {
		// TODO Auto-generated method stub
		return 0;
	}

	public long getBytesReceived(boolean reset) {
		// TODO Auto-generated method stub
		return 0;
	}

	public long gettotalBytesReceived() {
		// TODO Auto-generated method stub
		return 0;
	}

	public long getBuffOverflow(boolean reset) {
		// TODO Auto-generated method stub
		return 0;
	}

	public long getTotalBuffOverflow() {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean isConnected() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isRemoteAddress(String addr) {
		// TODO Auto-generated method stub
		return false;
	}

	public ByteBuffer read(ByteBuffer buff) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public void stop() throws IOException {
		// TODO Auto-generated method stub

	}

	public boolean waitingToSend() {
		// TODO Auto-generated method stub
		return false;
	}

	public int waitingToSendSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int write(ByteBuffer buff) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	public void setLogId(String logId) {
		// TODO Auto-generated method stub

	}

}
