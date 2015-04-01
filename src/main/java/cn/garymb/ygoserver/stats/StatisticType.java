/**
 * StatisticType.java
 * author: mabin
 * 2015年3月31日
 */
package cn.garymb.ygoserver.stats;

public enum StatisticType {
	QUEUE_WAITING("Total waiting packets", "int"),
	MAX_QUEUE_SIZE("Max queue size", "int"),
	MSG_RECEIVED_OK("Packets send", "long"),
	MSG_SENT_OK("Packets sent", "long"),
	IN_QUEUE_OVER("IN Queue overflow", "long"),
	OUT_QUEUE_OVERFLOW("OUT Queue overflow", "long"),
	LIST(null, "list"),
	OTHER(null, null);
	
	private String description = null;
	private String unit = null;
	
	private StatisticType(String description, String unit) {
		this.description = description;
		this.unit = unit;
	}
	
	public String getDescription() { return description; }
	public String getUnit() { return unit; }
}
