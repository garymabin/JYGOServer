package cn.garymb.ygoserver.ygo;

import java.nio.ByteBuffer;

import cn.garymb.ygoserver.ygo.ocgwrapper.type.GameMessage;

public class ReplaySavePacket extends YGOGamePacket {

	public ReplaySavePacket() {
		super(GameMessage.STOC_REPLAY);
		mBuffer = ByteBuffer.allocate(Replay.MAX_REPLAY_SIZE);
	}

}
