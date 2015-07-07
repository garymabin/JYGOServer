package cn.garymb.ygoserver.ygo.ocgwrapper.type;

public enum PlayerType {
	Undefined(-1),
	Player1(0),
	Player2(1),
	Player3(2),
	Player4(3),
	Player5(4),
	Player6(5),
	Observer(6),
	Host(0x10),
	Red(11),
	Green(12),
	Blue(13),
	Babyblue(14),
	Pink(15),
	Yellow(16),
	White(17),
	Grey(18);
	
	private int value;
	
	private PlayerType(int value) {
		this.value = value;
	}
	
	public int intValue() {
		return value;
	}
}
