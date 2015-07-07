/**
 * Packet.java
 * author: mabin
 * 2015年5月7日
 */
package cn.garymb.ygoserver.server;

import java.nio.ByteBuffer;
import java.util.UUID;

public class Packet {
	
	private static final int DEFAULT_S2C_PACKET_SIZE = 512;
	
	protected ByteBuffer mBuffer;
	private String mId;
	
	public Packet() {
		mBuffer = ByteBuffer.allocate(DEFAULT_S2C_PACKET_SIZE);
		mId = UUID.randomUUID().toString();
	}
	
	public Packet(byte[] content) {
		mBuffer = ByteBuffer.wrap(content);
		mId = UUID.randomUUID().toString();
	}
	
	public void write(byte[] content) {
		if (content == null) {
			return;
		}
		int remaining = mBuffer.remaining();
		if (remaining < content.length) {
			tryExtendBuffer(mBuffer, content.length - remaining);
		}
		mBuffer.put(content);
	}
	
	public void writeByte(int b) {
		mBuffer.put((byte) b);
	}
	
	public void writeInt16(int s) {
		mBuffer.putShort((short) s);
	}
	
	public void writeInt32(int s) {
		mBuffer.putInt(s);
	}

	private void tryExtendBuffer(ByteBuffer mBuffer2, int requiredSize) {
		//TODO
	}

	public byte[] readToEnd() {
		byte[] result = new byte[mBuffer.remaining()];
		mBuffer.get(result);
		return result;
	}
	
	public byte[] read(int length) {
		byte[] result = new byte[length];
		mBuffer.get(result);
		return result;
	}
	
	public int readInt16() {
		return mBuffer.getShort();
	}
	
	public int readInt32() {
		return mBuffer.getInt();
	}
	
	public int readByte() {
		return mBuffer.get();
	}
	
	public ByteBuffer getBuffer() {
		return mBuffer;
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
