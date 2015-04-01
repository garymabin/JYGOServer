/**
 * YGOCoreMain.java
 * author: mabin
 * 2015年3月30日
 */
package cn.garymb.ygoserver.server;

import cn.garymb.ygoserver.conf.ConfigManager;

public final class YGOCoreMain {
	
	public static void main(final String[] args) {
		parseParams(args);		
		start(args);
		
	}

	private static void start(String[] args) {
		Thread.setDefaultUncaughtExceptionHandler(new ThreadExcelptionHandler());
		System.out.println("Accepting client version 0x" + ConfigManager.peekInstance().getClientVersion() + " or higher.");
		
	}

	private static void parseParams(String[] args) {
	}
}
