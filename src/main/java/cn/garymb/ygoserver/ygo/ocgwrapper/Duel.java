/**
 * Duel.java
 * author: mabin
 * 2015年4月2日
 */
package cn.garymb.ygoserver.ygo.ocgwrapper;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import cn.garymb.ygoserver.ygo.ocgwrapper.type.CardLocation;
import cn.garymb.ygoserver.ygo.ocgwrapper.util.MTRandom;

/**
 * @author mabin
 *
 */
public final class Duel {
	
	private static final int BUFFER_SIZE = 4096;
	private ByteBuffer mBuffer;
	private static Map<Integer, Duel> sDuels;
	private int mDuelPtr;
	
	static {
		sDuels = new HashMap<Integer, Duel>();
	}
	
	
	private Duel(int duelPtr) {
		mBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
		mDuelPtr = duelPtr;
		sDuels.put(mDuelPtr, this);
	}
	
	
	public static Duel create(long seed) {
		MTRandom random = new MTRandom(seed);
		int duelPtr = OcgcoreBridge.create_duel(random.nextInt());
		return new Duel(duelPtr);
	}
	
	public void initPlayers(int startLP, int startHand, int drawCount) {
		OcgcoreBridge.set_player_info(mDuelPtr, 0, startLP, startHand, drawCount);
		OcgcoreBridge.set_player_info(mDuelPtr, 1, startLP, startHand, drawCount);
	}
	
	public void addCard(int cardId, int owner, CardLocation location) {
		OcgcoreBridge.new_card(mDuelPtr, cardId, (byte)owner, (byte)owner, location.byteValue(), (byte)0, (byte)0);
	}


	public void end() {
		//TODO
	}
}
