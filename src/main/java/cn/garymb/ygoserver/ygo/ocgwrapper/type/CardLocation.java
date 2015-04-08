/**
 * CardLocation.java
 * author: mabin
 * 2015年4月8日
 */
package cn.garymb.ygoserver.ygo.ocgwrapper.type;

public enum CardLocation {
	
	Deck(0x01),
	Hand(0x02),
	MonsterZone(0x04),
	SpellZone(0x08),
	Grave(0x10),
	Removed(0x20),
	Extra(0x40),
	Overlay(0x80),
	Onfield(0x0c);
	
	
	private int value;
	
	private CardLocation(int value) {
		this.value = value;
	}
	
	public byte byteValue() {
		return (byte) value;
	}
}
