/**
 * YGOGamePacket.java
 * author: mabin
 * 2015年5月7日
 */
package cn.garymb.ygoserver.ygo;

import cn.garymb.ygoserver.server.Packet;

public class YGOGamePacket extends Packet {
	
	public YGOGamePacket(YGOGamePacket p) {
		super(p);
	}
	
	public YGOGamePacket(byte[] content) {
		super(content);
	}
	
	public YGOGamePacket(int ctos) {
		super();
		writeStoc(ctos);
	}

	public int readMsgType() {
		mBuffer.rewind();
		return readByte();
	}
	
	public void writeStoc(int ctos) {
		mBuffer.clear();
		writeByte(ctos);
	}
}
