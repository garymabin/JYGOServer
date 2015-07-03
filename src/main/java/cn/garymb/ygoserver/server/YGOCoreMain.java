/**
 * YGOCoreMain.java
 * author: mabin
 * 2015年3月30日
 */
package cn.garymb.ygoserver.server;

import cn.garymb.ygoserver.conf.Configurator;

public final class YGOCoreMain {
	
	private static final String CONFIGURATOR_PROP_KEY = "ygoserver-configurator";
	
	private static final String DEF_CONFIGURATOR = "cn.garymb.ygoserver.Configurator";
	
	private static Configurator config;
	
	private static AbstractMessageReceiver amr;
	
	public static void main(final String[] args) {
		parseParams(args);		
		start(args);
		
	}

	private static void start(String[] args) {
		Thread.setDefaultUncaughtExceptionHandler(new ThreadExcelptionHandler());
		String config_class_name = System.getProperty( CONFIGURATOR_PROP_KEY,
				 DEF_CONFIGURATOR );
		try {
			config = (Configurator) Class.forName(config_class_name).newInstance();
			System.out.println("Accepting client version 0x" + config.getClientVersion() + " or higher.");
			
			String server_class_name = config.getYGOServerClassName();
			amr = (AbstractMessageReceiver) Class.forName(server_class_name).newInstance();
			amr.setName("ygoserver");
			amr.start();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void stop() {
		if (amr != null) {
			amr.stop();
		}
	}
	
	public static Configurator getConfigurator() {
		return config;
	}

	private static void parseParams(String[] args) {
	}
}
