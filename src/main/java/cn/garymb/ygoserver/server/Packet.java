/**
 * Packet.java
 * author: mabin
 * 2015年5月7日
 */
package cn.garymb.ygoserver.server;

import java.nio.ByteBuffer;
import java.util.UUID;

public class Packet {
	
	protected ByteBuffer mBuffer;
	private String mId;
	
	public Packet(byte[] content) {
		mBuffer = ByteBuffer.wrap(content);
		mId = UUID.randomUUID().toString();
	}

	public byte[] readToEnd() {
		byte[] result = new byte[mBuffer.remaining()];
		mBuffer.get(result);
		return result;
	}
	
	public byte[] getBytes() {
		return mBuffer.array();
	}

	public Enum<Priority> getPriority() {
		return Priority.NORMAL;
	}

	public boolean isCommand() {
		return false;
	}

	public String getId() {
		return mId;
	}

	public String getTo() {
		return null;
	}
	
	@Override
	public int hashCode() {
		return mId.hashCode();
	}

	public int getType() {
		return 0;
	}

}
