/**
 * BasicComponent.java
 * author: mabin
 * 2015年5月7日
 */
package cn.garymb.ygoserver.server;

import java.util.Queue;

public class BasicComponent implements ServerComponent {

	private String mName;
	private boolean initializationCompleted = false;

	/*
	 * (non-Javadoc)
	 * 
	 * @see cn.garymb.ygoserver.server.ServerComponent#initializationCompleted()
	 */
	public void initializationCompleted() {
		initializationCompleted = true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * cn.garymb.ygoserver.server.ServerComponent#processPacket(cn.garymb.ygoserver
	 * .server.Packet, java.util.Queue)
	 */
	public void processPacket(Packet packet, Queue<Packet> results) {
		if (packet.isCommand()) {
			processScriptCommand(packet, results);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see cn.garymb.ygoserver.server.ServerComponent#getName()
	 */
	public String getName() {
		return mName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * cn.garymb.ygoserver.server.ServerComponent#isInitializationComplete()
	 */
	public boolean isInitializationComplete() {
		return initializationCompleted;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see cn.garymb.ygoserver.server.ServerComponent#setName(java.lang.String)
	 */
	public void setName(String name) {
		mName = name;
	}

	protected boolean processScriptCommand(Packet pc, Queue<Packet> results) {
		return false;
	}

	public void release() {
	}

}
