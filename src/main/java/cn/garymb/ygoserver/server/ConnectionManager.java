/**
 * ConnectionManager.java
 * author: mabin
 * 2015年4月2日
 */
package cn.garymb.ygoserver.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import cn.garymb.ygoserver.net.ConnectionOpenListener;
import cn.garymb.ygoserver.net.ConnectionOpenThread;
import cn.garymb.ygoserver.net.ConnectionType;
import cn.garymb.ygoserver.net.SocketThread;
import cn.garymb.ygoserver.net.SocketType;
import cn.garymb.ygoserver.stats.StatisticList;
import cn.garymb.ygoserver.ygo.YGOIOService;
import cn.garymb.ygoserver.ygo.YGOIOServiceListener;

public abstract class ConnectionManager<IO extends YGOIOService<?>> extends
		AbstractMessageReceiver implements YGOIOServiceListener<IO>{

	private class ConnectionListenerImpl implements ConnectionOpenListener {
		private Map<String, Object> port_props = null;

		// ~--- constructors
		// -------------------------------------------------------

		private ConnectionListenerImpl(Map<String, Object> port_props) {
			this.port_props = port_props;
		}

		// ~--- methods
		// ------------------------------------------------------------

		/**
		 * Method description
		 *
		 *
		 * @param sc
		 */
		public void accept(SocketChannel sc) {
			String cid = "" + port_props.get("local-hostname") + "@"
					+ port_props.get("remote-hostname");

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Accept called for service: {0}", cid);
			}

			IO serv = getYGOIOService();

			serv.setIOServiceListener(ConnectionManager.this);
			serv.setSessionData(port_props);
			try {
				serv.accept(sc);
				if (getSocketType() == SocketType.ssl) {
					serv.startSSL(false, isTlsWantClientAuthEnabled());
				} // end of if (socket == SocketType.ssl)
				serviceStarted(serv);
				SocketThread.addSocketService(serv);
			} catch (Exception e) {
				if (getConnectionType() == ConnectionType.connect) {

					// Accept side for component service is not ready yet?
					// Let's wait for a few secs and try again.
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST,
								"Problem reconnecting the service: {0}, cid: {1}",
								new Object[] { serv, cid });
					}
					updateConnectionDetails(port_props);

					boolean reconnect = false;
					Integer reconnects = (Integer) port_props
							.get(MAX_RECONNECTS_PROP_KEY);

					if (reconnects != null) {
						int recon = reconnects.intValue();

						if (recon != 0) {
							port_props.put(MAX_RECONNECTS_PROP_KEY, (--recon));
							reconnect = true;
						} // end of if (recon != 0)
					}
					if (reconnect) {
						reconnectService(port_props, connectionDelay);
					} else {
						reconnectionFailed(port_props);
					}
				} else {

					// Ignore
				}

				// } catch (Exception e) {
				// if (log.isLoggable(Level.FINEST)) {
				// log.log(Level.FINEST, "Can not accept connection cid: " +
				// cid, e);
				// }
				// log.log(Level.WARNING, "Can not accept connection.", e);
				// serv.stop();
			} // end of try-catch
		}

		/**
		 * Method description
		 *
		 *
		 *
		 *
		 * @return a value of <code>String</code>
		 */
		@Override
		public String toString() {
			return port_props.toString();
		}

		// ~--- get methods
		// --------------------------------------------------------

		/**
		 * Method description
		 *
		 *
		 *
		 *
		 * @return a value of <code>ConnectionType</code>
		 */
		public ConnectionType getConnectionType() {
			String type = null;

			if (port_props.get(PORT_TYPE_PROP_KEY) == null) {
				log.warning(getName() + ": connection type is null: "
						+ port_props.get(PORT_KEY).toString());
			} else {
				type = port_props.get(PORT_TYPE_PROP_KEY).toString();
			}

			return ConnectionType.valueOf(type);
		}

		/**
		 * Method description
		 *
		 *
		 *
		 *
		 * @return a value of <code>String[]</code>
		 */
		public String[] getIfcs() {
			return (String[]) port_props.get(PORT_IFC_PROP_KEY);
		}

		/**
		 * Method description
		 *
		 *
		 *
		 *
		 * @return a value of <code>int</code>
		 */
		public int getPort() {
			return (Integer) port_props.get(PORT_KEY);
		}

		/**
		 * Method description
		 *
		 *
		 *
		 *
		 * @return a value of <code>int</code>
		 */
		public int getReceiveBufferSize() {
			return net_buffer;
		}

		/**
		 * Method description
		 *
		 *
		 *
		 *
		 * @return a value of <code>InetSocketAddress</code>
		 */
		public InetSocketAddress getRemoteAddress() {
			return (InetSocketAddress) port_props.get("remote-address");
		}

		/**
		 * Method description
		 *
		 *
		 *
		 *
		 * @return a value of <code>String</code>
		 */
		public String getRemoteHostname() {
			if (port_props.containsKey(PORT_REMOTE_HOST_PROP_KEY)) {
				return (String) port_props.get(PORT_REMOTE_HOST_PROP_KEY);
			}

			return (String) port_props.get("remote-hostname");
		}

		/**
		 * Method description
		 *
		 *
		 *
		 *
		 * @return a value of <code>SocketType</code>
		 */
		public SocketType getSocketType() {
			return SocketType.valueOf(port_props.get(PORT_SOCKET_PROP_KEY)
					.toString());
		}

		/**
		 * Method description
		 *
		 *
		 *
		 *
		 * @return a value of <code>String</code>
		 */
		public String getSRVType() {
			String type = (String) this.port_props.get("srv-type");

			if ((type == null) || type.isEmpty()) {
				return null;
			}

			return type;
		}

		/**
		 * Method description
		 *
		 *
		 *
		 *
		 * @return a value of <code>int</code>
		 */
		public int getTrafficClass() {
			if (isHighThroughput()) {
				return IPTOS_THROUGHPUT;
			} else {
				return DEF_TRAFFIC_CLASS;
			}
		}
	}

	private static final Logger log = Logger.getLogger(ConnectionManager.class
			.getName());

	protected static final long LAST_MINUTE_PACKETS_LIMIT_PROP_VAL = 2500L;
	protected static final long LAST_MINUTE_BIN_LIMIT_PROP_VAL = 20000000L;
	protected static final long TOTAL_BIN_LIMIT_PROP_VAL = 0L;
	protected static final long TOTAL_PACKETS_LIMIT_PROP_VAL = 0L;
	protected static final int NET_BUFFER_ST_PROP_VAL = 2 * 1024;

	protected static final String PORT_REMOTE_HOST_PROP_KEY = "remote-host";
	protected static final String PORT_KEY = "port-no";
	protected static final String PORT_SOCKET_PROP_KEY = "socket";
	protected static final String MAX_RECONNECTS_PROP_KEY = "max-reconnects";
	protected static final String PORT_TYPE_PROP_KEY = "type";
	protected static final String PORT_IFC_PROP_KEY = "ifc";

	protected long connectionDelay = 2 * SECOND;
	protected int net_buffer = NET_BUFFER_ST_PROP_VAL;

	private static ConnectionOpenThread connectThread = ConnectionOpenThread
			.getInstance();

	private ConcurrentHashMap<String, IO> services = new ConcurrentHashMap<String, IO>();

	private LinkedList<Map<String, Object>> waitingTasks = new LinkedList<Map<String, Object>>();

	private Set<ConnectionListenerImpl> pending_open = Collections
			.synchronizedSet(new HashSet<ConnectionListenerImpl>());;

	private long total_packets_limit = TOTAL_PACKETS_LIMIT_PROP_VAL;
	private long total_bin_limit = TOTAL_BIN_LIMIT_PROP_VAL;
	private long last_minute_packets_limit = LAST_MINUTE_PACKETS_LIMIT_PROP_VAL;
	private long last_minute_bin_limit = LAST_MINUTE_BIN_LIMIT_PROP_VAL;

	private long bytesReceived = 0;
	private long bytesSent = 0;
	private int services_size = 0;
	private long socketOverflow = 0;

	private boolean initializationCompleted = false;

	private IOServiceStatisticsGetter ioStatsGetter = new IOServiceStatisticsGetter();

	public static enum LIMIT_ACTION {
		DISCONNECT, DROP_PACKETS;
	}

	private LIMIT_ACTION limitAction = LIMIT_ACTION.DISCONNECT;

	@Override
	public void processPacket(Packet packet) {

	}

	public boolean checkTrafficLimits(IO serv) {
		boolean isLimitHit = false;

		if (last_minute_packets_limit > 0) {
			isLimitHit = (serv.getPacketsReceived(false) >= last_minute_packets_limit)
					|| (serv.getPacketsSent(false) >= last_minute_packets_limit);
		}
		if (!isLimitHit && (total_packets_limit > 0)) {
			isLimitHit = (serv.getTotalPacketsReceived() >= total_packets_limit)
					|| (serv.getTotalPacketsSent() >= total_packets_limit);
		}
		if (isLimitHit) {
			Level level = Level.FINER;

			if (isHighThroughput()) {
				level = Level.WARNING;
			}
			switch (limitAction) {
			case DROP_PACKETS:
				if (log.isLoggable(level)) {
					log.log(level,
							"[[{0}]] XMPP Limits exceeded on connection {1}"
									+ " dropping pakcets: {2}",
							new Object[] { getName(), serv,
									serv.getReceivedPackets() });
				}
				while (serv.getReceivedPackets().poll() != null)
					;

				break;

			default:
				if (log.isLoggable(level)) {
					log.log(level,
							"[[{0}]] XMPP Limits exceeded on connection {1}"
									+ " stopping, packets dropped: {2}",
							new Object[] { getName(), serv,
									serv.getReceivedPackets() });
				}
				serv.forceStop();

				break;
			}

			return false;
		}

		boolean binLimitHit = false;
		long bytesSent = serv.getBytesSent(false);
		long bytesReceived = serv.getBytesReceived(false);

		if (last_minute_bin_limit > 0) {
			binLimitHit = (bytesSent >= last_minute_bin_limit)
					|| (bytesReceived >= last_minute_bin_limit);
		}

		long totalSent = serv.getTotalBytesSent();
		long totalReceived = serv.getTotalBytesReceived();

		if (!binLimitHit && (total_bin_limit > 0)) {
			binLimitHit = (totalReceived >= total_bin_limit)
					|| (totalSent >= total_bin_limit);
		}
		if (binLimitHit) {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE,
						"[[{0}]] Binary Limits exceeded ({1}:{2}:{3}:{4}) on"
								+ " connection {5} stopping, packets dropped: {6}",
						new Object[] { getName(), bytesSent, bytesReceived,
								totalSent, totalReceived, serv,
								serv.getReceivedPackets() });
			}
			serv.forceStop();

			return false;
		}

		return true;
	}

	@Override
	public synchronized void everyMinute() {
		super.everyMinute();
		int tmp = services.size();

		services_size = tmp;
		doForAllServices(ioStatsGetter);
	}

	public boolean isHighThroughput() {
		// TODO Auto-generated method stub
		return false;
	}
	
	protected abstract IO getYGOIOService();
	
	public abstract Queue<Packet> processSocketData(IO serv);
	
	public abstract void reconnectionFailed(Map<String, Object> port_props);

	protected void doForAllServices(ServiceChecker<IO> checker) {
		for (IO service : services.values()) {
			checker.check(service);
		}
	}

	private class IOServiceStatisticsGetter implements ServiceChecker<IO> {
		private StatisticList list = new StatisticList(Level.ALL);

		// ~--- methods
		// ------------------------------------------------------------

		/**
		 * Method description
		 *
		 *
		 * @param service
		 */
		public synchronized void check(IO service) {
			bytesReceived += service.getBytesReceived(true);
			bytesSent += service.getBytesSent(true);
			socketOverflow += service.getBuffOverflow(true);
			service.getPacketsReceived(true);
			service.getPacketsSent(true);

			// service.getStatistics(list, true);
			// bytesReceived += list.getValue("socketio", "Bytes received",
			// -1l);
			// bytesSent += list.getValue("socketio", "Bytes sent", -1l);
			// socketOverflow += list.getValue("socketio", "Buffers overflow",
			// -1l);
		}
	}

	@Override
	public void initializationCompleted() {
		if (isInitializationComplete()) {

			// Do we really need to do this again?
			return;
		}
		super.initializationCompleted();
		initializationCompleted = true;
		for (Map<String, Object> params : waitingTasks) {
			reconnectService(params, connectionDelay);
		}
		waitingTasks.clear();
	}
	
	public void packetsReady(IO serv) throws IOException {
		if (checkTrafficLimits(serv)) {
			writePacketsToSocket(serv, processSocketData(serv));
		}		
	}
	
	/**
	 * Method description
	 *
	 *
	 * @param serv
	 * @param packets
	 */
	public void writePacketsToSocket(IO serv, Queue<Packet> packets) {
		if (serv != null) {

			// synchronized (serv) {
			if ((packets != null) && (packets.size() > 0)) {
				Packet p = null;

				while ((p = packets.poll()) != null) {
					if (log.isLoggable(Level.FINER) &&!log.isLoggable(Level.FINEST)) {
						log.log(Level.FINER, "{0}, Processing packet: type: {1}", new Object[] {
								serv, p.getType() });
					}
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "{0}, Writing packet: {1}", new Object[] { serv, p });
					}
					serv.addPacketToSend(p);
				}      // end of for ()
				try {
					serv.processWaitingPackets();
					SocketThread.addSocketService(serv);
				} catch (Exception e) {
					log.log(Level.WARNING, serv + "Exception during writing packets: ", e);
					try {
						serv.stop();
					} catch (Exception e1) {
						log.log(Level.WARNING, serv + "Exception stopping XMPPIOService: ", e1);
					}    // end of try-catch
				}      // end of try-catch
			}

			// }
		} else {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Can't find service for packets: [{0}] ", packets);
			}
		}          // end of if (ios != null) else
	}
	
	public boolean writePacketToSocket(IO ios, Packet p) {
		if (ios != null) {
			if (log.isLoggable(Level.FINER) &&!log.isLoggable(Level.FINEST)) {
				log.log(Level.FINER, "{0}, Processing packet: type: {1}", new Object[] { ios, p.getType() });
			}
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "{0}, Writing packet: {1}", new Object[] { ios, p });
			}

			// synchronized (ios) {
			ios.addPacketToSend(p);
			if (ios.writeInProgress.tryLock()) {
				try {
					ios.processWaitingPackets();
					SocketThread.addSocketService(ios);

					return true;
				} catch (Exception e) {
					log.log(Level.WARNING, ios + "Exception during writing packets: ", e);
					try {
						ios.stop();
					} catch (Exception e1) {
						log.log(Level.WARNING, ios + "Exception stopping XMPPIOService: ", e1);
					}    // end of try-catch
				} finally {
					ios.writeInProgress.unlock();
				}
			}

			// }
		} else {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Can''t find service for packet: <{0}> {1}",
						new Object[] { p.toString(),
						p.getTo() });
			}
		}    // end of if (ios != null) else

		return false;
	}
	
	public void updateConnectionDetails(Map<String, Object> port_props) {}

	private void reconnectService(final Map<String, Object> port_props,
			long delay) {
		if (log.isLoggable(Level.FINER)) {
			String cid = "" + port_props.get("local-hostname") + "@"
					+ port_props.get("remote-hostname");

			log.log(Level.FINER,
					"Reconnecting service for: {0}, scheduling next try in {1}secs, cid: {2}",
					new Object[] { getName(), delay / 1000, cid });
		}
		addTimerTask(new cn.garymb.ygoserver.util.TimerTask() {

			public void run() {
				String host = (String) port_props.get(PORT_REMOTE_HOST_PROP_KEY);

				if (host == null) {
					host = (String) port_props.get("remote-hostname");
				}

				int port = (Integer) port_props.get(PORT_KEY);

				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE,
							"Reconnecting service for component: {0}, to remote host: {1} on port: {2}",
							new Object[] { getName(), host, port });
				}
				startService(port_props);
			}
		}, delay);
	}

	private void startService(Map<String, Object> port_props) {
		if (port_props == null) {
			throw new NullPointerException("port_props cannot be null.");
		}

		ConnectionListenerImpl cli = new ConnectionListenerImpl(port_props);

		if (cli.getConnectionType() == ConnectionType.accept) {
			pending_open.add(cli);
		}
		connectThread.addConnectionOpenListener(cli);
	}
	
	public void serviceStarted(final IO service) {

		// synchronized(services) {
		String id = getUniqueId(service);

		if (log.isLoggable(Level.FINER)) {
			log.log(Level.FINER, "[[{0}]] Connection started: {1}", new Object[] { getName(),
					service });
		}

		IO serv = services.get(id);

		if (serv != null) {
			if (serv == service) {
				log.log(Level.WARNING,
						"{0}: That would explain a lot, adding the same service twice, ID: {1}",
						new Object[] { getName(),
						serv });
			} else {

				// Is it at all possible to happen???
				// let's log it for now....
				log.log(Level.WARNING,
						"{0}: Attempt to add different service with the same ID: {1}", new Object[] {
						getName(),
						service });

				// And stop the old service....
				serv.stop();
			}
		}
		services.put(id, service);
		++services_size;

		// }
	}

	protected String getUniqueId(IO service) {
		return service.getUniqueId();
	}
	
	protected boolean isTlsWantClientAuthEnabled() {
		return false;
	}


}
