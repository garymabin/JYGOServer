/**
 * IOService.java
 * author: mabin
 * 2015年4月1日
 */
package cn.garymb.ygoserver.net;

import java.io.IOException;

public interface IOServiceListener<IO extends IOService<?>> {
	void packetsReady(IO service) throws IOException;

	boolean serviceStopped(IO service);
}
