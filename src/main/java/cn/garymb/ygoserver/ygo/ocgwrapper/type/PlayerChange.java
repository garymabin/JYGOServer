package cn.garymb.ygoserver.ygo.ocgwrapper.type;

public enum PlayerChange {
	
	Observe(0x8),
	Ready(0x9),
	NotReady(0xA),
	Leave(0xB);
	
	private int value;
	
	private PlayerChange(int value) {
		this.value = value;
	}
	
	public int intValue() {
		return value;
	}
}
