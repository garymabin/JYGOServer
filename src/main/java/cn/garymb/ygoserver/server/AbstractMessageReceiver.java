/**
 * AbstractMessageReceiver.java
 * author: mabin
 * 2015年5月7日
 */
package cn.garymb.ygoserver.server;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import cn.garymb.ygoserver.stats.StatisticList;
import cn.garymb.ygoserver.stats.StatisticType;
import cn.garymb.ygoserver.stats.StatisticsContainer;
import cn.garymb.ygoserver.util.PriorityQueueAbstract;

public abstract class AbstractMessageReceiver extends BasicComponent implements
		MessageReceiver, StatisticsContainer {

	private class PacketReceiverTask extends cn.garymb.ygoserver.util.TimerTask {
		private ReceiverTimeoutHandler handler = null;
		private String id = null;
		private Packet packet = null;

		// ~--- constructors
		// -------------------------------------------------------

		private PacketReceiverTask(ReceiverTimeoutHandler handler, long delay,
				TimeUnit unit, Packet packet) {
			super();
			this.handler = handler;
			this.packet = packet;
			id = packet.getId();
			waitingTasks.put(id, this);
			addTimerTask(this, delay, unit);

			// log.finest("[" + getName() + "]  " + "Added timeout task for: " +
			// id);
		}

		// ~--- methods
		// ------------------------------------------------------------

		/**
		 * Method description
		 *
		 *
		 * @param response
		 */
		public void handleResponse(Packet response) {

			// waitingTasks.remove(packet.getFrom() + packet.getId());
			this.cancel();

			// log.finest("[" + getName() + "]  " + "Response received for id: "
			// +
			// id);
			handler.responseReceived(packet, response);
		}

		/**
		 * Method description
		 *
		 */
		public void handleTimeout() {

			// log.finest("[" + getName() + "]  " + "Fired timeout for id: " +
			// id);
			waitingTasks.remove(id);
			handler.timeOutExpired(packet);
		}

		/**
		 * Method description
		 *
		 */
		public void run() {
			handleTimeout();
		}
	}

	public class QueueListener extends Thread {

		private QueueType type;
		private PriorityQueueAbstract<Packet> queue;
		@SuppressWarnings("unused")
		private String compName;
		private boolean threadStopped = false;
		private long packetCounter = 0;

		public QueueListener(PriorityQueueAbstract<Packet> q, QueueType type) {
			this.queue = q;
			this.type = type;
			compName = AbstractMessageReceiver.this.getName();
		}

		@Override
		public void run() {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "{0} starting queue processing.",
						getName());
			}

			Packet packet = null;
			Queue<Packet> results = new ArrayDeque<Packet>(2);

			while (!threadStopped) {
				try {

					// Now process next waiting packet
					// log.finest("[" + getName() + "] before take... " + type);
					// packet = queue.take(getName() + ":" + type);
					packet = queue.take();
					++packetCounter;

					// if (log.isLoggable(Level.INFO)) {
					// log.info("[" + getName() + "] packet from " + type +
					// " queue: " +
					// packet);
					// }
					switch (type) {
					case IN_QUEUE:
						long startPPT = System.currentTimeMillis();

						// tracer.trace(null, packet.getElemTo(),
						// packet.getElemFrom(),
						// packet.getFrom(), getName(), type.name(), null,
						// packet);
						PacketReceiverTask task = null;

						if (packet.getTo() != null) {
							String id = packet.getTo().toString()
									+ packet.getId();

							task = waitingTasks.remove(id);
						}
						if (task != null) {
							task.handleResponse(packet);
						} else {

							// log.finest("[" + getName() + "]  " +
							// "No task found for id: " + id);
							// Maybe this is a command for local processing...
							boolean processed = false;

							if (packet.isCommand()) {
								processed = processScriptCommand(packet,
										results);
								if (processed) {
									Packet result = null;

									while ((result = results.poll()) != null) {
										addOutPacket(result);
									}
								}
							}
							if (!processed
									&& ((packet = filterPacket(packet,
											incoming_filters)) != null)) {
								processPacket(packet);
							}

							// It is all concurrent so we have to use a local
							// index variable
							int idx = pptIdx;

							pptIdx = (pptIdx + 1) % processPacketTimings.length;

							long timing = System.currentTimeMillis() - startPPT;

							processPacketTimings[idx] = timing;
						}

						break;

					case OUT_QUEUE:

						// tracer.trace(null, packet.getElemTo(),
						// packet.getElemFrom(),
						// packet.getTo(), getName(), type.name(), null,
						// packet);
						if ((packet = filterPacket(packet, outgoing_filters)) != null) {
							processOutPacket(packet);
						}

						break;

					default:
						log.log(Level.SEVERE,
								"Unknown queue element type: {0}", type);

						break;
					} // end of switch (qel.type)
				} catch (InterruptedException e) {

					// log.log(Level.SEVERE,
					// "Exception during packet processing: ", e);
					// stopped = true;
				} catch (Exception e) {
					log.log(Level.SEVERE,
							"[" + getName()
									+ "] Exception during packet processing: "
									+ packet, e);
				} // end of try-catch
			}
		}

	}

	public static final Integer MAX_QUEUE_SIZE_PROP_VAL = new Long(Runtime
			.getRuntime().maxMemory() / 400000L).intValue();

	private static final Logger log = Logger
			.getLogger("tigase.debug.AbstractMessageReceiver");

	protected static final long SECOND = 1000;

	protected static final long MINUTE = 60 * SECOND;

	protected static final long HOUR = 60 * MINUTE;

	protected int maxInQueueSize = MAX_QUEUE_SIZE_PROP_VAL;
	protected int maxOutQueueSize = MAX_QUEUE_SIZE_PROP_VAL;

	private int pptIdx = 0;
	private int in_queues_size = 1;
	private int out_queues_size = 1;
	private long last_hour_packets = 0;
	private long last_minute_packets = 0;
	private long last_second_packets = 0;
	private int schedulerThreads_size = 1;
	private long statReceivedPacketsOk = 0;

	private long statSentPacketsEr = 0;
	private long statSentPacketsOk = 0;
	private long statReceivedPacketsEr = 0;

	private long packets_per_hour = 0;
	private long packets_per_minute = 0;
	private long packets_per_second = 0;

	private MessageReceiver parent = null;

	private final Priority[] pr_cache = Priority.values();
	private final List<PriorityQueueAbstract<Packet>> in_queues = new ArrayList<PriorityQueueAbstract<Packet>>(
			pr_cache.length);

	private final List<PriorityQueueAbstract<Packet>> out_queues = new ArrayList<PriorityQueueAbstract<Packet>>(
			pr_cache.length);

	private final CopyOnWriteArrayList<PacketFilterIfc> incoming_filters = new CopyOnWriteArrayList<PacketFilterIfc>();
	private final CopyOnWriteArrayList<PacketFilterIfc> outgoing_filters = new CopyOnWriteArrayList<PacketFilterIfc>();

	private final ConcurrentHashMap<String, PacketReceiverTask> waitingTasks = new ConcurrentHashMap<String, PacketReceiverTask>(
			16, 0.75f, 4);
	private ScheduledExecutorService receiverScheduler = null;
	private Timer receiverTasks = null;
	private QueueListener out_thread = null;

	private ArrayDeque<QueueListener> threadsQueue = null;

	private final long[] processPacketTimings = new long[100];

	public void processPacket(Packet packet, Queue<Packet> results) {
		addPacketNB(packet);
	}

	public void setName(String name) {
		super.setName(name);
		in_queues_size = processingInThreads();
		out_queues_size = processingOutThreads();
		schedulerThreads_size = schedulerThreads();
		setMaxQueueSize(maxInQueueSize);
	}

	public boolean addPacket(Packet packet) {
		int queueIdx = Math.abs(hashCodeForPacket(packet) % in_queues_size);
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "[{0}] queueIdx={1}, {2}", new Object[] {
					getName(), queueIdx, packet.toString() });
		}
		try {
			in_queues.get(queueIdx).put(packet, packet.getPriority().ordinal());
			++statReceivedPacketsOk;
		} catch (InterruptedException e) {
			++statReceivedPacketsEr;
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Packet dropped for unknown reason: {0}",
						packet);
			}

			return false;
		} // end of try-catch
		return true;
	}

	public boolean addPacketNB(Packet packet) {
		int queueIdx = Math.abs(hashCodeForPacket(packet) % in_queues_size);

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "[{0}] queueIdx={1}, {2}", new Object[] {
					getName(), queueIdx, packet.toString() });
		}

		boolean result = in_queues.get(queueIdx).offer(packet,
				packet.getPriority().ordinal());

		if (result) {
			++statReceivedPacketsOk;
		} else {

			// Queue overflow!
			++statReceivedPacketsEr;
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST,
						"Packet dropped due to queue overflow: {0}", packet);
			}
		}

		return result;
	}

	public boolean addPackets(Queue<Packet> packets) {
		boolean result = true;
		Packet p = packets.peek();

		while (p != null) {
			result = addPacket(p);
			if (result) {
				packets.poll();
			} else {
				break;
			} // end of if (result) else
			p = packets.peek();
		} // end of while ()

		return result;
	}
	

	public void processOutPacket(Packet packet) {
		if (parent != null) {
			parent.addPacket(packet);
		} else {

			// It may happen for MessageRouter and this is intentional
			addPacketNB(packet);

			// log.warning("[" + getName() + "]  " + "No parent!");
		} // end of else
	}

	public void setMaxQueueSize(int maxQueueSize) {
		if ((this.maxInQueueSize != maxQueueSize) || (in_queues.size() == 0)) {

			// out_queue =
			// PriorityQueueAbstract.getPriorityQueue(pr_cache.length,
			// maxQueueSize);
			// Processing threads number is split to incoming and outgoing
			// queues...
			// So real processing threads number of in_queues is
			// processingThreads()/2
			this.maxInQueueSize = (maxQueueSize / processingInThreads()) * 2;
			this.maxOutQueueSize = (maxQueueSize / processingOutThreads()) * 2;
			if (in_queues.size() == 0) {
				for (int i = 0; i < in_queues_size; i++) {
					PriorityQueueAbstract<Packet> queue = PriorityQueueAbstract
							.getPriorityQueue(pr_cache.length, maxQueueSize);

					in_queues.add(queue);
				}
			} else {
				for (int i = 0; i < in_queues.size(); i++) {
					in_queues.get(i).setMaxSize(maxQueueSize);
				}
			}
			if (out_queues.size() == 0) {
				for (int i = 0; i < out_queues_size; i++) {
					PriorityQueueAbstract<Packet> queue = PriorityQueueAbstract
							.getPriorityQueue(pr_cache.length, maxQueueSize);

					out_queues.add(queue);
				}
			} else {
				for (int i = 0; i < out_queues.size(); i++) {
					out_queues.get(i).setMaxSize(maxQueueSize);
				}
			}

			// out_queue.setMaxSize(maxQueueSize);
		} // end of if (this.maxQueueSize != maxQueueSize)
	}

	public int processingOutThreads() {
		return 1;
	}

	public int processingInThreads() {
		return 1;
	}

	public int schedulerThreads() {
		return 1;
	}

	public void addTimerTask(cn.garymb.ygoserver.util.TimerTask task, long delay) {
		ScheduledFuture<?> future = receiverScheduler.schedule(task, delay,
				TimeUnit.MILLISECONDS);
		task.setScheduledFuture(future);
	}

	protected void addTimerTask(cn.garymb.ygoserver.util.TimerTask task,
			long delay, TimeUnit unit) {
		ScheduledFuture<?> future = receiverScheduler.schedule(task, delay,
				unit);

		task.setScheduledFuture(future);
	}

	public abstract void processPacket(Packet packet);

	protected boolean addOutPacket(Packet packet) {
		int queueIdx = Math.abs(hashCodeForPacket(packet) % out_queues_size);

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "[{0}]  queueIdx={1}, {2}", new Object[] {
					getName(), queueIdx, packet.toString() });
		}
		try {
			out_queues.get(queueIdx)
					.put(packet, packet.getPriority().ordinal());
			++statSentPacketsOk;
		} catch (InterruptedException e) {
			++statSentPacketsEr;
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Packet dropped for unknown reason: {0}",
						packet);
			}

			return false;
		} // end of try-catch

		return true;
	}

	private Packet filterPacket(Packet packet,
			CopyOnWriteArrayList<PacketFilterIfc> filters) {
		Packet result = packet;

		for (PacketFilterIfc packetFilterIfc : filters) {
			result = packetFilterIfc.filter(result);
			if (result == null) {
				break;
			}
		}

		return result;
	}

	public void start() {
		if (log.isLoggable(Level.FINER)) {
			log.log(Level.INFO, "{0}: starting queue management threads ...",
					getName());
		}
		startThreads();
	}

	public void stop() {
		if (log.isLoggable(Level.FINER)) {
			log.log(Level.INFO, "{0}: stopping queue management threads ...",
					getName());
		}
		stopThreads();
	}
	
	@Override
	public void release() {
		stop();
	}

	private void stopThreads() {
		// stopped = true;
		try {
			if (threadsQueue != null) {
				for (QueueListener in_thread : threadsQueue) {
					in_thread.threadStopped = true;
					in_thread.interrupt();
					while (in_thread.isAlive()) {
						Thread.sleep(100);
					}
				}
			}
			if (out_thread != null) {
				out_thread.threadStopped = true;
				out_thread.interrupt();
				while (out_thread.isAlive()) {
					Thread.sleep(100);
				}
			}
		} catch (InterruptedException e) {
		}
		threadsQueue = null;
		out_thread = null;
		if (receiverTasks != null) {
			receiverTasks.cancel();
			receiverTasks = null;
		}
		if (receiverScheduler != null) {
			receiverScheduler.shutdownNow();
			receiverScheduler = null;
		}
	}

	private void startThreads() {
		if (threadsQueue == null) {
			threadsQueue = new ArrayDeque<QueueListener>(8);
			for (int i = 0; i < in_queues_size; i++) {
				QueueListener in_thread = new QueueListener(in_queues.get(i),
						QueueType.IN_QUEUE);

				in_thread.setName("in_" + i + "-" + getName());
				in_thread.start();
				threadsQueue.add(in_thread);
			}
			for (int i = 0; i < out_queues_size; i++) {
				QueueListener out_thread = new QueueListener(out_queues.get(i),
						QueueType.OUT_QUEUE);

				out_thread.setName("out_" + i + "-" + getName());
				out_thread.start();
				threadsQueue.add(out_thread);
			}
		} // end of if (thread == null || ! thread.isAlive())

		// if ((out_thread == null) ||!out_thread.isAlive()) {
		// out_thread = new QueueListener(out_queue, QueueType.OUT_QUEUE);
		// out_thread.setName("out_" + getName());
		// out_thread.start();
		// } // end of if (thread == null || ! thread.isAlive())
		receiverScheduler = Executors
				.newScheduledThreadPool(schedulerThreads_size);
		receiverTasks = new Timer(getName() + " tasks", true);
		receiverTasks.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				everySecond();
			}
		}, SECOND, SECOND);
		receiverTasks.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				everyMinute();
			}
		}, MINUTE, MINUTE);
		receiverTasks.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				everyHour();
			}
		}, HOUR, HOUR);
	}

	private int hashCodeForPacket(Packet packet) {
		return packet.hashCode();
	}

	public synchronized void everySecond() {
		packets_per_second = statReceivedPacketsOk - last_second_packets;
		last_second_packets = statReceivedPacketsOk;
	}

	public synchronized void everyMinute() {
		packets_per_minute = statReceivedPacketsOk - last_minute_packets;
		last_minute_packets = statReceivedPacketsOk;
		receiverTasks.purge();
	}

	public synchronized void everyHour() {
		packets_per_hour = statReceivedPacketsOk - last_hour_packets;
		last_hour_packets = statReceivedPacketsOk;
	}

	public void getStatistics(StatisticList list) {
		list.add(getName(), "Last second packets", packets_per_second,
				Level.FINE);
		list.add(getName(), "Last minute packets", packets_per_minute,
				Level.FINE);
		list.add(getName(), "Last hour packets", packets_per_hour, Level.FINE);
		list.add(getName(), "Processing threads", processingInThreads(),
				Level.FINER);
		list.add(getName(), StatisticType.MSG_RECEIVED_OK.getDescription(),
				statReceivedPacketsOk, Level.FINE);
		list.add(getName(), StatisticType.MSG_SENT_OK.getDescription(),
				statSentPacketsOk, Level.FINE);
		if (list.checkLevel(Level.FINEST)) {
			int[] in_priority_sizes = in_queues.get(0).size();

			for (int i = 1; i < in_queues.size(); i++) {
				int[] tmp_pr_sizes = in_queues.get(i).size();

				for (int j = 0; j < tmp_pr_sizes.length; j++) {
					in_priority_sizes[j] += tmp_pr_sizes[j];
				}
			}

			int[] out_priority_sizes = out_queues.get(0).size();

			for (int i = 1; i < out_queues.size(); i++) {
				int[] tmp_pr_sizes = out_queues.get(i).size();

				for (int j = 0; j < tmp_pr_sizes.length; j++) {
					out_priority_sizes[j] += tmp_pr_sizes[j];
				}
			}
			for (int i = 0; i < in_priority_sizes.length; i++) {
				Priority queue = Priority.values()[i];

				list.add(getName(), "In queue wait: " + queue.name(),
						in_priority_sizes[queue.ordinal()], Level.FINEST);
			}
			for (int i = 0; i < out_priority_sizes.length; i++) {
				Priority queue = Priority.values()[i];

				list.add(getName(), "Out queue wait: " + queue.name(),
						out_priority_sizes[queue.ordinal()], Level.FINEST);
			}
		}

		int in_queue_size = 0;

		for (PriorityQueueAbstract<Packet> total_size : in_queues) {
			in_queue_size += total_size.totalSize();
		}

		int out_queue_size = 0;

		for (PriorityQueueAbstract<Packet> total_size : out_queues) {
			out_queue_size += total_size.totalSize();
		}
		list.add(getName(), "Total In queues wait", in_queue_size, Level.INFO);
		list.add(getName(), "Total Out queues wait", out_queue_size, Level.INFO);
		list.add(getName(), "Total queues wait",
				(in_queue_size + out_queue_size), Level.INFO);
		list.add(getName(), StatisticType.MAX_QUEUE_SIZE.getDescription(),
				(maxInQueueSize * processingInThreads()), Level.FINEST);
		list.add(getName(), StatisticType.IN_QUEUE_OVERFLOW.getDescription(),
				statReceivedPacketsEr, Level.INFO);
		list.add(getName(), StatisticType.OUT_QUEUE_OVERFLOW.getDescription(),
				statSentPacketsEr, Level.INFO);
		list.add(getName(), "Total queues overflow",
				(statReceivedPacketsEr + statSentPacketsEr), Level.INFO);

		long res = 0;

		for (long ppt : processPacketTimings) {
			res += ppt;
		}

		long prcessingTime = res / processPacketTimings.length;

		list.add(getName(), "Average processing time on last "
				+ processPacketTimings.length + " runs [ms]", prcessingTime,
				Level.FINE);
		for (PacketFilterIfc packetFilter : incoming_filters) {
			packetFilter.getStatistics(list);
		}
		for (PacketFilterIfc packetFilter : outgoing_filters) {
			packetFilter.getStatistics(list);
		}
		if (list.checkLevel(Level.FINEST)) {
			for (QueueListener thread : threadsQueue) {
				list.add(getName(),
						"Processed packets thread: " + thread.getName(),
						thread.packetCounter, Level.FINEST);
			}
		}
	}

}
