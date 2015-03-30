/**
 * ThreadExcelptionHandler.java
 * author: mabin
 * 2015年3月30日
 */
package cn.garymb.ygoserver.server;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ThreadExcelptionHandler implements UncaughtExceptionHandler {
	
	private static final Logger log = Logger.getLogger("cn.garymb.ygoserver.ThreadExceptionHandler");

	public void uncaughtException(Thread t, Throwable e) {
		log.log(Level.SEVERE, "Uncaught thread:\"" + t.getName() + "\" exception", e);
	}

}
