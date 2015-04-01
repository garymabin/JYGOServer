/**
 * IOInterface.java
 * author: mabin
 * 2015年3月31日
 */
package cn.garymb.ygoserver.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import cn.garymb.ygoserver.stats.StatisticList;

public interface IOInterface {
	int bytesRead();
	
	boolean checkCapabilities(String caps);
	
	int getInputPacketSize() throws IOException;
	
	SocketChannel getSocketChannel();
	
	void getStatistics(StatisticList list, boolean reset);
	
	long getBytesSent(boolean reset);
	
	long getTotalBytesSent();
	
	long getBytesReceived(boolean reset);
	
	long gettotalBytesReceived();
	
	long getBuffOverflow(boolean reset);
	
	long getTotalBuffOverflow();
	
	boolean isConnected();
	
	boolean isRemoteAddress(String addr);
	
	ByteBuffer read(final ByteBuffer buff) throws IOException;
	
	void stop() throws IOException;
	
	boolean waitingToSend();
	
	int waitingToSendSize();
	
	int write(final ByteBuffer buff) throws IOException;
	
	void setLogId(String logId);
}
