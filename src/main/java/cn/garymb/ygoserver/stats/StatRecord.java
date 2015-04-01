/**
 * StatRecord.java
 * author: mabin
 * 2015年3月31日
 */
package cn.garymb.ygoserver.stats;

import java.util.List;
import java.util.logging.Level;

public class StatRecord {
	private StatisticType type = StatisticType.OTHER;
	
	private Level level = Level.INFO;
	
	private long longValue = -1;
	private int intValue = -1;
	private float floatValue = -1f;
	
	private String description = null;
	private String unit = null;
	private String value = null;
	private List<String> listValue = null;
	private String component = null;
	
	public StatRecord(String comp, String description, String unit, String value, Level level) {
		this.description = description;
		this.unit = unit;
		this.value = value;
		this.level = level;
		this.component = comp;
	}
	
	public StatRecord(String comp, StatisticType type, long value, Level level) {
		this(comp, type.getDescription(), type.getUnit(), "" + value, level);
		this.type = type;
		this.longValue = value;
	}
	
	public StatRecord(String comp, StatisticType type, int value, Level level) {
		this(comp, type.getDescription(), type.getUnit(), "" + value, level);
		this.type = type;
		this.intValue = value;
	}
	
	public StatRecord(String comp, StatisticType type, List<String> value, Level level) {
		this(comp, type.getDescription(), type.getUnit(), "" + value, level);
		this.type = type;
		this.listValue = value;
	}
	
	StatRecord(String comp, String description, String unit, float value, Level level) {
		this(comp, description, unit,  "" + value, level);
		this.floatValue = value;
	}
	
	public String getDescription() {
		return description;
	}
	
	public String getUnit() {
		return unit;
	}
	
	public String getValue() {
		return value;
	}
	
	public StatisticType getType() {
		return type;
	}
	
	public List<String> getListValue() {
		return listValue;
	}
	
	public Level getLevel() {
		return level;
	}
	
	public String getComponent() {
		return component;
	}
	
	public long getLongValue() {
		return this.longValue;
	}
	
	public int getIntValue() {
		return this.intValue;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(component).append('/').append(description);
		sb.append('[').append(unit).append(']').append(" = ").append(value);
		return sb.toString();
	}
	
	float getFloatValue() {
		return this.floatValue;
	}
}
