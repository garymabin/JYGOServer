/**
 * ConfigManager.java
 * author: mabin
 * 2015年3月30日
 */
package cn.garymb.ygoserver.conf;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ConfigManager {
	private static final Logger log = Logger.getLogger("cn.garymb.ygoserver.ConfigManager");
	
	private static final String CONFIGFILE = System.getProperty("user.dir") + File.separator + "conf//ygoserver.properties";
	
	private File mFile;
	
	private Properties mProperties;
	
	private static ConfigManager INSTANCE;
	
	private ConfigManager() {
		mFile = new File(CONFIGFILE);
		if (!mFile.exists()) {
			log.log(Level.WARNING, "config file does not exist.");
		}
		mProperties = new Properties();
		try {
			mProperties.load(new FileInputStream(mFile));
		} catch (Exception e) {
			log.log(Level.SEVERE, "load properties faile.");
		}
	}
	
	public static ConfigManager peekInstance() {
		if (INSTANCE == null) {
			INSTANCE = new ConfigManager();
		}
		return INSTANCE;
	}
	
	private int getIntegerValue(String key, int defValue) {
		Object propValue = getConfigItem(key, defValue);
		return Integer.valueOf((String)propValue);
	}
	
	private String getStringValue(String key, String defValue) {
		return (String)getConfigItem(key, defValue);
	}
	
	private boolean getBooleanValue(String key, boolean defValue) {
		Object propValue = getConfigItem(key, defValue ? 1 : 0);
		return Integer.valueOf((String)propValue) > 0;	
	}
	
	final private Object getConfigItem(String name, Object defaultVal) {
		Object val = mProperties.getProperty(name);
		return val == null ? defaultVal :val;
	}
	
	public int getServerPort() {
		return getIntegerValue(PropertyKey.SERVER_PORT, PropertyKey.SERVER_PORT_DEF_VALUE);
	}
	
	public String getPath() {
		return getStringValue(PropertyKey.PATH, PropertyKey.PATH_DEF_VALUE);
	}
	
	public String getScriptsDir() {
		return getStringValue(PropertyKey.SCRIPTS_DIR, PropertyKey.SCRIPTS_DIR_DEF_VALUE);
	}
	
	public String getDataBasePath() {
		return getStringValue(PropertyKey.CARD_DB_FILE, PropertyKey.CARD_DB_FILE_DEF_VALUE);
	}
	
	public String getBanlistFile() {
		return getStringValue(PropertyKey.BANLIST_FILE, PropertyKey.BANLIST_FILE_DEF_VALUE);
	}
	
	public String getClientVersion() {
		return getStringValue(PropertyKey.CLIENT_VERSION, PropertyKey.CLIENT_VERSION_DEF_VALUE);
	}
	
	public boolean getHandShuffleConfig() {
		return getBooleanValue(PropertyKey.HAND_SHUFFLE, PropertyKey.HAND_SHUFFLE_DEF_VALUE);
	}
	
	public boolean getAutoEndTurnConfig() {
		return getBooleanValue(PropertyKey.AUTO_END_TURN, PropertyKey.AUTO_END_TURN_DEF_VALUE);
	}
	
	public int getOperationTimeout() {
		return getIntegerValue(PropertyKey.OP_TIMEOUT, PropertyKey.OP_TIMEOUT_DEF_VALUE);
	}
}
