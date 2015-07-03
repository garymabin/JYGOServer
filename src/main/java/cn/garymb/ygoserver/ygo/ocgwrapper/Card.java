/**
 * Card.java
 * author: mabin
 * 2015年4月7日
 */
package cn.garymb.ygoserver.ygo.ocgwrapper;

public class Card {
	
	public int code;
	public int alias;
	public long setcode;
	public int type;
	public int level;
	public int attr;
	public int race;
	public int attack;
	public int defense;
	public int lscale;
	public int rscale;
	
	private int id;
	private int ot;
	
	public Card(int id, int ot) {
		this.setId(id);
		this.setOt(ot);
	}
	
	public int getId() {
		return id;
	}

	private void setId(int id) {
		this.id = id;
	}

	public int getOt() {
		return ot;
	}

	private void setOt(int ot) {
		this.ot = ot;
	}

}
