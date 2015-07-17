package cn.garymb.ygoserver.ygo;

import cn.garymb.ygoserver.server.Packet;

public interface IGameAnalyser {
	int processMessage(Packet p);
}
