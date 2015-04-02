package cn.garymb.ygoserver.ygo;

import java.io.IOException;

import cn.garymb.ygoserver.net.IOService;

public class YGOIOService<RefObject> extends IOService<RefObject> {

	@Override
	protected void processSocketData() throws IOException {
		
	}

	@Override
	protected int receivedPackets() {
		return 0;
	}

}
