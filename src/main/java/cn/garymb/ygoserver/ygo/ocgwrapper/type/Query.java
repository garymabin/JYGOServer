/**
 * Query.java
 * author: mabin
 * 2015年4月8日
 */
package cn.garymb.ygoserver.ygo.ocgwrapper.type;

public class Query {
	public static final int CODE = 0x01;
	public static final int POSITION = 0x02;
	public static final int ALIAS = 0x04;
	public static final int TYPE = 0x08;
	public static final int LEVEL = 0x10;
	public static final int RANK = 0x20;
	public static final int ATTRIBUTE = 0x40;
	public static final int RACE = 0x80;
	public static final int ATTACK = 0x100;
	public static final int DEFENCE = 0x200;
	public static final int BASE_ATTACK = 0x400;
	public static final int BASE_DEFENCE = 0x800;
	public static final int REASON = 0x1000;
	public static final int REASON_CARD = 0x2000;
	public static final int EQUIP_CARD = 0x4000;
	public static final int TARGET_CARD = 0x8000;
	public static final int OVERLAY_CARD = 0x10000;
	public static final int COUNTERS = 0x20000;
	public static final int OWNER = 0x40000;
	public static final int IS_DISABLED = 0x80000;
	public static final int IS_PUBLIC = 0x100000;
	public static final int LSCALE = 0x200000;
	public static final int RSCALE = 0x400000;
}
