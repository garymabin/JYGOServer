/**
 * YGOGamePacket.java
 * author: mabin
 * 2015年5月7日
 */
package cn.garymb.ygoserver.ygo;

import cn.garymb.ygoserver.server.Packet;

public class YGOGamePacket extends Packet {
	
	public YGOGamePacket(byte[] content) {
		super(content);
	}
	
	public int readCtos() {
		return mBuffer.get();
	}
}
