/**
 * OcgcoreBridge.java
 * author: mabin
 * 2015年4月2日
 */
package cn.garymb.ygoserver.ygo.ocgwrapper;

import java.nio.ByteBuffer;

public final class OcgcoreBridge {
	static {
		System.loadLibrary("libocgcore");
	}

	public static native void set_script_reader(Object classobj);
	
	public static native void set_card_reader(Object classobj);
	
	public static native void set_message_handler(Object classobj);
	
	public static native int create_duel(int seed);
	
	public static native void start_duel(int duelptr, int options);
	
	public static native void end_duel(int duelptr);
	
	public static native void set_player_info(int duelPtr, int playerId, int lp, int startcount, int drawcount);
	
	public static native int get_log_message(int duelPtr, ByteBuffer buf);
	
	public static native int get_message(int duelPtr, ByteBuffer buf);
	
	public static native int process(int duelPtr);
	
	public static native void new_card(int duelPtr, int code, byte owner, byte playerid, byte loc, byte seq, byte pos);
	
	public static native void new_tag_card(int duelPtr, int code, byte owner, byte loc);
	
	public static native int query_card(int duelPtr, byte playerid,  byte loc, byte seq, int query_flag, ByteBuffer buf, int use_cache);
	
	public static native int query_field_count(int duelPtr, byte playerid, byte loc);
	
	public static native int query_field_card(int duelPtr, byte playerid, byte loc, int query_flag, ByteBuffer buf, int use_cache); 
	
	public static native int query_field_info(int duelPtr, ByteBuffer buf);
	
	public static native void set_responsei(int duelPtr, int value);
	
	public static native void set_responseb(int duelPtr, ByteBuffer buf);
	
	public static native int preload_script(int duelPtr, ByteBuffer buf, int len);
	
}
