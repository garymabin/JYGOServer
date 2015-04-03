/**
 * OcgcoreNativeHandler.java
 * author: mabin
 * 2015年4月2日
 */
package cn.garymb.ygoserver.ygo.ocgwrapper;

import java.nio.ByteBuffer;


public final class OcgcoreNativeHandler {
	private OcgcoreNativeHandler() {
	}
	
	public static ByteBuffer scriptReader(ByteBuffer buffer, int ptr) {
		return null;
	}
	
	public static int cardReader(int type, int ptr) {
		return -1;
	}
	
	public static int messageHandler(int ptr, int type) {
		return -1;
	}
}
