package cn.garymb.ygoserver.net;

import java.nio.channels.Selector;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConnectionOpenThread implements Runnable {
	
	private static class PortThrottlingData {
		public PortThrottlingData() {
		}
		protected long lasgSecondConnections = 0;
		
		protected long throttling;
	}

	public static final long def_throttling = 200;
	
	private static ConnectionOpenThread acceptThread = null;
	private static final Logger log = Logger.getLogger(ConnectionOpenThread.class.getName());
	public static PortThrottlingData throttling = new PortThrottlingData();
	
	protected long accept_counter = 0;
	private Selector selctor = null;
	private boolean stopping = false;
	private Timer timer = null;
	
	private ConnectionOpenThread() {
		timer = new Timer("Connections open timer", true);
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				throttling.lasgSecondConnections = 0;
			}
		}, 1000, 1000);
		try {
			selctor = Selector.open();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Server I/O error, can't continue my wokr");
			stopping = true;
		}
	}

	public void run() {
	}

}
