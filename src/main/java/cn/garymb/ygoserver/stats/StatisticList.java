/**
 * StatisticList.java
 * author: mabin
 * 2015年4月1日
 */
package cn.garymb.ygoserver.stats;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import cn.garymb.ygoserver.util.DataTypes;

public class StatisticList implements Iterable<StatRecord> {
	private static final Logger log = Logger.getLogger(StatisticList.class
			.getName());

	private Level statLevel = Level.ALL;
	private LinkedHashMap<String, LinkedHashMap<String, StatRecord>> stats = new LinkedHashMap<String, LinkedHashMap<String, StatRecord>>();
	
	public StatisticList(Level level) {
		this.statLevel = level;
	}
	
	public boolean add(String comp, String description, int value, Level recordLevel) {
		if (checkLevel(recordLevel, value)) {
			LinkedHashMap<String, StatRecord> compStats = stats.get(comp);

			if (compStats == null) {
				compStats = addCompStats(comp);
			}

			compStats.put(description, new StatRecord(comp, description, "long", value,
					recordLevel));

			return true;
		}

		return false;
	}
	
	public boolean add(String comp, String description, float value, Level recordLevel) {
		if (checkLevel(recordLevel)) {
			LinkedHashMap<String, StatRecord> compStats = stats.get(comp);

			if (compStats == null) {
				compStats = addCompStats(comp);
			}

			compStats.put(description, new StatRecord(comp, description, "float", value,
					recordLevel));

			return true;
		}

		return false;
	}
	
	public boolean add(String comp, String description, long value, Level recordLevel) {
		if (checkLevel(recordLevel)) {
			LinkedHashMap<String, StatRecord> compStats = stats.get(comp);

			if (compStats == null) {
				compStats = addCompStats(comp);
			}

			compStats.put(description, new StatRecord(comp, description, "float", value,
					recordLevel));

			return true;
		}

		return false;
	}
	
	public boolean add(String comp, String description, String value, Level recordLevel) {
		if (checkLevel(recordLevel)) {
			LinkedHashMap<String, StatRecord> compStats = stats.get(comp);

			if (compStats == null) {
				compStats = addCompStats(comp);
			}

			compStats.put(description, new StatRecord(comp, description, "float", value,
					recordLevel));

			return true;
		}

		return false;
	}
	
	public LinkedHashMap<String, StatRecord> addCompStats(String comp) {
		LinkedHashMap<String, StatRecord> compStats = new LinkedHashMap<String, StatRecord>();

		stats.put(comp, compStats);

		return compStats;
	}
	
	public boolean checkLevel(Level recordLevel) {
		return recordLevel.intValue() >= statLevel.intValue();
	}
	
	public boolean checkLevel(Level recordLevel, long value) {
		if (checkLevel(recordLevel)) {
			if (value == 0) {
				return checkLevel(Level.FINEST);
			}
		}
		return false;
	}
	
	public boolean checkLevel(Level recordLevel, int value) {
		if (checkLevel(recordLevel)) {
			if (value == 0) {
				return checkLevel(Level.FINEST);
			}
		}
		return false;
	}
	
	public int getCompConnections(String comp) {
		return getValue(comp, "Open connections", 0);
	}
	
	public long getCompPackets(String comp) {
		return getCompSentPackets(comp) + getCompReceivedPackets(comp);
	}
	
	public long getCompReceivedPackets(String comp) {
		return getValue(comp, StatisticType.MSG_RECEIVED_OK.getDescription(), 0L);
	}
	
	public long getCompSentPackets(String comp) {
		return getValue(comp, StatisticType.MSG_SENT_OK.getDescription(), 0L);
	}
	
	
	public LinkedHashMap<String, StatRecord> getCompStats(String comp) {
		return stats.get(comp);
	}
	
	public long getValue(String comp, String description, long def) {
		long result = def;
		LinkedHashMap<String, StatRecord> compStats = stats.get(comp);
		
		if (compStats != null) {
			StatRecord rec = compStats.get(description);
			
			if (rec != null) {
				result = rec.getLongValue();
			}
		}
		return result;
	}
	
	public float getValue(String comp, String description, float def) {
		float result = def;
		LinkedHashMap<String, StatRecord> compStats = stats.get(comp);
		
		if (compStats != null) {
			StatRecord rec = compStats.get(description);
			
			if (rec != null) {
				result = rec.getFloatValue();
			}
		}
		return result;
	}
	
	public int getValue(String comp, String description, int def) {
		int result = def;
		LinkedHashMap<String, StatRecord> compStats = stats.get(comp);
		
		if (compStats != null) {
			StatRecord rec = compStats.get(description);
			
			if (rec != null) {
				result = rec.getIntValue();
			}
		}
		return result;
	}
	
	public String getValue(String comp, String description, String def) {
		String result = def;
		LinkedHashMap<String, StatRecord> compStats = stats.get(comp);
		
		if (compStats != null) {
			StatRecord rec = compStats.get(description);
			
			if (rec != null) {
				result = rec.getValue();
			}
		}
		return result;
	}
	
	public Object getValue(String dataId) {
		char dataType = DataTypes.decodeTypeIdFromName(dataId);
		String dataName = DataTypes.stripNameFromTypeId(dataId);
		int idx = dataName.indexOf('/');
		String comp = dataName.substring(0, idx);
		String descr = dataName.substring(idx + 1);
		log.log(Level.FINER,
				"Returning metrics for component: {0}, description: {1} and type: {2}",
				new Object[] { comp, descr, dataType });
		switch (dataType) {
			case 'L':
				return getValue(comp, descr, 0l);
			case 'I':
				return getValue(comp, descr, 0);
			case 'F':
				return getValue(comp, descr, 0f);
			default:
				return getValue(comp, descr, " ");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<StatRecord> iterator() {
		return new StatsIterator();
	}
	
	private class StatsIterator implements Iterator<StatRecord> {
		Iterator<LinkedHashMap<String, StatRecord>> compsIt = stats.values().iterator();
		Iterator<StatRecord> recIt = null;

		// ~--- get methods --------------------------------------------------------

		/**
		 * Method description
		 * 
		 * 
		 * 
		 */
		public boolean hasNext() {
			if ((recIt == null) || !recIt.hasNext()) {
				if (compsIt.hasNext()) {
					recIt = compsIt.next().values().iterator();
				} else {
					return false;
				}
			}

			return recIt.hasNext();
		}

		// ~--- methods ------------------------------------------------------------

		/**
		 * Method description
		 * 
		 * 
		 * 
		 * 
		 * @throws NoSuchElementException
		 */
		public StatRecord next() throws NoSuchElementException {
			if ((recIt == null) || !recIt.hasNext()) {
				if (compsIt.hasNext()) {
					recIt = compsIt.next().values().iterator();
				} else {
					throw new NoSuchElementException("No more statistics.");
				}
			}

			return recIt.next();
		}

		/**
		 * Method description
		 * 
		 */
		public void remove() {
			throw new UnsupportedOperationException("Not supported yet.");
		}
	}

}
