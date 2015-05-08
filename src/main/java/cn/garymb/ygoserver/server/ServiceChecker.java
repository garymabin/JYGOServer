/**
 * ServiceChecker.java
 * author: mabin
 * 2015年5月8日
 */
package cn.garymb.ygoserver.server;

import cn.garymb.ygoserver.ygo.YGOIOService;

public interface ServiceChecker<IO extends YGOIOService<?>> {
	void check(IO service);
}
