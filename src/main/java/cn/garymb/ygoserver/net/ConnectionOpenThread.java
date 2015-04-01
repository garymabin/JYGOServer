/**
 * ConnectionOpenThread.java
 * author: mabin
 * 2015年3月30日
 */
package cn.garymb.ygoserver.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConnectionOpenThread implements Runnable {

	public static class PortThrottlingData {
		public PortThrottlingData(long port_throttling) {
			throttling = port_throttling;
		}

		protected long lasgSecondConnections = 0;

		protected long throttling;
	}

	public static final long def_throttling = 200;

	private static ConnectionOpenThread acceptThread = null;
	private static final Logger log = Logger
			.getLogger(ConnectionOpenThread.class.getName());
	public static Map<Integer, PortThrottlingData> throttling = new ConcurrentHashMap<Integer, PortThrottlingData>(
			10);

	private ConcurrentLinkedQueue<ConnectionOpenListener> waiting = new ConcurrentLinkedQueue<ConnectionOpenListener>();

	protected long accept_counter = 0;
	private Selector selector = null;
	private boolean stopping = false;
	private Timer timer = null;
	
	public static ConnectionOpenThread getInstance() {
		if (acceptThread == null) {
			acceptThread = new ConnectionOpenThread();
			Thread thrd = new Thread(acceptThread);
			
			thrd.setName("ConnectionOpenThread");
			thrd.start();
			if (log.isLoggable(Level.FINER)) {
				log.finer("ConnectionOpenThread started");
			}
		}
		return acceptThread;
 	}

	private ConnectionOpenThread() {
		timer = new Timer("Connections open timer", true);
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				for (PortThrottlingData portThrottlingData : throttling
						.values()) {
					portThrottlingData.lasgSecondConnections = 0;
				}
			}
		}, 1000, 1000);
		try {
			selector = Selector.open();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Server I/O error, can't continue my wokr");
			stopping = true;
		}
	}

	public void addConnectionOpenListener(ConnectionOpenListener al) {
		waiting.offer(al);
		selector.wakeup();
	}

	public void removeConnectionOpenListner(ConnectionOpenListener al) {
		for (SelectionKey key : selector.keys()) {
			if (al == key.attachment()) {
				try {
					key.cancel();
					SelectableChannel channel = key.channel();

					channel.close();
				} catch (Exception e) {
					log.log(Level.WARNING,
							"Exception during removing connection listener.", e);
				}
				break;
			}
		}
	}

	public void run() {
		while (!stopping) {
			try {
				selector.select();
				for (Iterator i = selector.selectedKeys().iterator(); i
						.hasNext();) {
					SelectionKey sk = (SelectionKey) i.next();
					i.remove();
					SocketChannel sc = null;
					boolean throttled = false;
					int port_no = 0;

					if ((sk.readyOps() & SelectionKey.OP_ACCEPT) != 0) {
						ServerSocketChannel nextReady = (ServerSocketChannel) sk
								.channel();
						port_no = nextReady.socket().getLocalPort();
						sc = nextReady.accept();
						if (log.isLoggable(Level.FINEST)) {
							log.finest("OP_ACCEPT");
						}
						PortThrottlingData port_throttling = throttling
								.get(port_no);
						if (port_throttling != null) {
							++port_throttling.lasgSecondConnections;
							if (port_throttling.lasgSecondConnections > port_throttling.throttling) {
								if (log.isLoggable(Level.FINER)) {
									log.log(Level.FINER,
											"New connections throttling level exceeded, closing: {0}, sc");
								}
								sc.close();
								sc = null;
								throttled = true;
							}
						} else {
							log.log(Level.WARNING,
									"Throttling not configured for port: {0}",
									port_no);
						}
					}
					if ((sk.readyOps() & SelectionKey.OP_CONNECT) != 0) {
						sk.cancel();
						sc = (SocketChannel) sk.channel();
						if (log.isLoggable(Level.FINEST)) {
							log.finest("OP_CONNECT");
						}
					}
					if (sc != null) {
						try {
							sc.configureBlocking(false);
							sc.socket().setSoLinger(false, 0);
							sc.socket().setReuseAddress(true);
							if (log.isLoggable(Level.FINER)) {
								log.log(Level.FINER,
										"Registered new client socket: {0}", sc);
							}

							ConnectionOpenListener al = (ConnectionOpenListener) sk
									.attachment();

							sc.socket().setTrafficClass(al.getTrafficClass());
							sc.socket().setReceiveBufferSize(
									al.getReceiveBufferSize());
							al.accept(sc);
						} catch (java.net.SocketException e) {
							log.log(Level.INFO,
									"Socket closed instantlu  after it had been opened?",
									e);
						}
					} else {
						log.log(Level.WARNING,
								"Can't  obtain socket channel from selection ket, throttling activated = {0}, for port: {1}",
								new Object[] { throttled, port_no });
					}
					++accept_counter;
				}
				addAllWaiting();
			} catch (IOException e) {
				log.log(Level.SEVERE, "Server I/O error", e);
			} catch (Exception e) {
				log.log(Level.SEVERE, "Othrer service exception.", e);
			}
		}
	}

	public void start() {
		Thread t = new Thread(this);
		t.setName("ConnectionOpenThread");
		t.start();
	}

	public void stop() {
		stopping = true;
		selector.wakeup();
	}

	private void addAllWaiting() {
		ConnectionOpenListener al = null;
		while ((al = waiting.poll()) != null) {
			try {
				addPort(al);
			} catch (Exception e) {
				log.log(Level.WARNING, "Error: creating connection for: " + al,
						e);
				al.accept(null);
			}
		}
	}

	private void addPort(ConnectionOpenListener al) throws IOException {
		if ((al.getConnectionType() == ConnectionType.connect)
				&& (al.getRemoteAddress() != null)) {
			addISA(al.getRemoteAddress(), al);
		} else if ((al.getIfcs() == null)
				|| (al.getIfcs().length == 0)
				|| (al.getIfcs()[0].equals("ifc") || al.getIfcs()[0]
						.equals("*"))) {
			addISA(new InetSocketAddress(al.getPort()), al);
		} else {
			for (String ifc : al.getIfcs()) {
				addISA(new InetSocketAddress(ifc, al.getPort()), al);
			}
		}

	}

	private void addISA(InetSocketAddress isa,
			ConnectionOpenListener al) throws IOException{
		switch (al.getConnectionType()) {
		case accept:
			long port_throttling = def_throttling;
			throttling.put(isa.getPort(), new PortThrottlingData(port_throttling));
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Setting up throttling for the port {0} to {1} connections per second.", new Object[]{isa.getPort(), port_throttling});
			}
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Setting up 'accept' channel...");
			}
			
			ServerSocketChannel ssc = ServerSocketChannel.open();
			
			ssc.socket().setReceiveBufferSize(al.getReceiveBufferSize());
			ssc.configureBlocking(false);
			ssc.socket().bind(isa);
			ssc.register(selector, SelectionKey.OP_ACCEPT, al);
			
			break;

		case connect:
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Setting up ''connect'' channel for: {0}/{1}", new Object[] {isa.getAddress(), isa.getPort()});
			}
			
			SocketChannel sc = SocketChannel.open();
			
			sc.socket().setReceiveBufferSize(al.getReceiveBufferSize());
			sc.socket().setTrafficClass(al.getTrafficClass());
			sc.configureBlocking(false);
			sc.connect(isa);
			sc.register(selector, SelectionKey.OP_CONNECT, al);
			
			break;
			
		default:
			log.log(Level.WARNING, "UNknown connection type: {0}", al.getConnectionType());
			
			break;
		}
	}
}
