package cn.garymb.ygoserver.ygo;

import java.util.ArrayList;
import java.util.List;

public class Banlist {
	private List<Integer> mBannedIds;
	private List<Integer> mLimitedIds;
	private List<Integer> mSemiLimitedIds;

	private int mHash = 0x7dfcee6a;

	public Banlist() {
		mBannedIds = new ArrayList<Integer>();
		mLimitedIds = new ArrayList<Integer>();
		mSemiLimitedIds = new ArrayList<Integer>();
	}

	public int getQuantity(int cardId) {
		if (mBannedIds.contains(cardId)) {
			return 0;
		}
		if (mLimitedIds.contains(cardId)) {
			return 1;
		}
		if (mSemiLimitedIds.contains(cardId)) {
			return 2;
		}
		return 3;
	}

	public void add(int cardId, int quantity) {
		if (quantity < 0 || quantity > 2) {
			return;
		}
		switch (quantity) {
		case 0:
			mBannedIds.add(cardId);
		case 1:
			mLimitedIds.add(cardId);
		case 2:
			mSemiLimitedIds.add(cardId);
			break;
		default:
			break;
		}
		mHash = mHash ^ ((cardId << 18) | (cardId >> 14)) ^ ((cardId << (27 + quantity)) | (cardId >> (5 - quantity)));
	}
	
	@Override
	public int hashCode() {
		return mHash;
	}
}
