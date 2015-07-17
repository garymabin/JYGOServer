/**
 * Duel.java
 * author: mabin
 * 2015年4月2日
 */
package cn.garymb.ygoserver.ygo.ocgwrapper;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import cn.garymb.ygoserver.ygo.IGameAnalyser;
import cn.garymb.ygoserver.ygo.YGOGamePacket;
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

	private IGameAnalyser mAnalyser;

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
		OcgcoreBridge.new_card(mDuelPtr, cardId, (byte) owner, (byte) owner, (byte) location.intValue(), (byte) 0,
				(byte) 0);
	}

	public void addTagCard(int id, int pid, CardLocation location) {
		OcgcoreBridge.new_tag_card(mDuelPtr, id, (byte) pid, (byte) location.intValue());
	}

	public void end() {
		OcgcoreBridge.end_duel(mDuelPtr);
	}

	public int queryFieldCount(int i, CardLocation location) {
		return OcgcoreBridge.query_field_count(mDuelPtr, (byte) i, (byte) location.intValue());
	}

	public byte[] queryFieldCard(int player, CardLocation location, int flag, boolean useCache) {
		mBuffer.clear();
		int len = OcgcoreBridge.query_field_card(mDuelPtr, (byte) player, (byte) location.intValue(), flag, mBuffer,
				useCache ? 1 : 0);
		byte[] result = new byte[len];
		mBuffer.flip();
		mBuffer.get(result);
		return result;
	}

	public void start(int options) {
		OcgcoreBridge.start_duel(mDuelPtr, options);
	}

	public int process() {
		int result = OcgcoreBridge.process(mDuelPtr);
		int len = result & 0xffff;
		if (len > 0) {
			mBuffer.clear();
			OcgcoreBridge.get_message(mDuelPtr, mBuffer);
			// maybe we should add some exception handle
			YGOGamePacket packet = new YGOGamePacket(mBuffer.array());
			if (mAnalyser != null) {
				return mAnalyser.processMessage(packet);
			}
		}
		return -1;
	}

	public void setAnalyser(IGameAnalyser analyser) {
		mAnalyser = analyser;
	}

	public void setResponse(byte[] resp) {
		OcgcoreBridge.set_responseb(mDuelPtr,ByteBuffer.wrap(resp));
	}

}
