/**
 * StatisticsContainer.java
 * author: mabin
 * 2015年5月7日
 */
package cn.garymb.ygoserver.stats;

import cn.garymb.ygoserver.server.ServerComponent;

public interface StatisticsContainer extends ServerComponent {
	  public void getStatistics(StatisticList list);
}
