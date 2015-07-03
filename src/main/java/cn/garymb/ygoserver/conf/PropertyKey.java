/**
 * PropertyKey.java
 * author: mabin
 * 2015年3月30日
 */
package cn.garymb.ygoserver.conf;

public final class PropertyKey {
	
	public static final String SERVER_PORT = "serverport";
	
	public static final String PATH = "path";
	
	public static final String SCRIPTS_DIR = "scriptdir";
	
	public static final String CARD_DB_FILE = "carddbfile";
	
	public static final String BANLIST_FILE = "banlistfile";
	
	public static final String CLIENT_VERSION = "clientversion";
	
	public static final String HAND_SHUFFLE = "handshuffle";
	
	public static final String AUTO_END_TURN = "autoendturn";
	
	public static final String OP_TIMEOUT = "timeout";
	
	public static final int SERVER_PORT_DEF_VALUE = 7911;
	
	public static final String PATH_DEF_VALUE = "./";
	
	public static final String SCRIPTS_DIR_DEF_VALUE = "scripts";
	
	public static final String CARD_DB_FILE_DEF_VALUE = "cards.db";
	
	public static final String BANLIST_FILE_DEF_VALUE = "lflist";
	
	public static final String CLIENT_VERSION_DEF_VALUE = "0x1034";
	
	public static final boolean HAND_SHUFFLE_DEF_VALUE = false;
	
	public static final boolean AUTO_END_TURN_DEF_VALUE = true;
	
	public static final int OP_TIMEOUT_DEF_VALUE = 180;
	
	public static final String DEF_CLIENTS_MANAGER_CLASS_NAME = "cn.garymb.ygoserver.ygo.YGOClientsManager";
}
