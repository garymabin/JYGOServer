/**
 * YGOIOServiceListener.java
 * author: mabin
 * 2015年5月8日
 */
package cn.garymb.ygoserver.ygo;

import cn.garymb.ygoserver.net.IOServiceListener;

public interface YGOIOServiceListener<IO extends YGOIOService<?>> extends
		IOServiceListener<IO> {

}
