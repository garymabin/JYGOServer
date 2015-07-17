package cn.garymb.ygoserver.ygo;

import java.lang.ref.WeakReference;

import cn.garymb.ygoserver.server.Packet;
import cn.garymb.ygoserver.ygo.ocgwrapper.type.GameMessage;

public class YGODuelAnalyser implements IGameAnalyser {
	
	private WeakReference<Game> mAttachedGame;
	
	public YGODuelAnalyser(Game game) {
		mAttachedGame = new WeakReference<Game>(game);
	}

	public int processMessage(Packet p) {
		int msgType = ((YGOGamePacket) p).readMsgType();
		Game game = mAttachedGame.get();
		switch (msgType) {
		case GameMessage.RETRY:
		case GameMessage.HINT:
		case GameMessage.WIN:
		case GameMessage.SELECT_BATTLE_CMD:
		case GameMessage.SELECT_IDLE_CMD:
		case GameMessage.SELECT_EFFECT_YN:
		case GameMessage.SELECT_YES_NO:
		case GameMessage.SELECT_OPTION:
		case GameMessage.SELECT_CARD:
		case GameMessage.SELECT_TRIBUTE:
		case GameMessage.SELECT_CHAIN:
		case GameMessage.SELECT_PLACE:
		case GameMessage.SELECT_DISFIELD:
		case GameMessage.SELECT_POSITION:
		case GameMessage.SELECT_COUNTER:
		case GameMessage.SELECT_SUM:
		case GameMessage.SORT_CARD:
		case GameMessage.SORT_CHAIN:
		case GameMessage.CONFIRM_DESKTOP:
		case GameMessage.CONFIRM_CARDS:
		case GameMessage.SHUFFLE_DECK:
		case GameMessage.SHUFFLE_HAND:
		case GameMessage.SWAP_GRAVE_DECK:
		case GameMessage.REVERSE_DECK:
		case GameMessage.DECKTOP:
		case GameMessage.SHUFFLE_SET_CARD:
		case GameMessage.NEW_TURN:
		case GameMessage.NEW_PHASE:
		case GameMessage.MOVE:
		case GameMessage.POS_CHANGE:
		case GameMessage.SET:
		case GameMessage.SWAP:
		case GameMessage.FIELD_DISABLED:
		case GameMessage.SUMMONED:
		case GameMessage.SP_SUMMONED:
		case GameMessage.FLIP_SUMMONED:
		case GameMessage.SUMMONING:
		case GameMessage.SP_SUMMONING:
		case GameMessage.FLIP_SUMMONING:
		case GameMessage.CHAINING:
		case GameMessage.CHAINED:
		case GameMessage.CHAIN_SOLVING:
		case GameMessage.CHAIN_SOLVED:
		case GameMessage.CHAIN_END:
		case GameMessage.CHAIN_NEGATED:
		case GameMessage.CHAIN_DISABLED:
		case GameMessage.CARD_SELECTED:
		case GameMessage.RANDOM_SELECTED:
		case GameMessage.BECOME_TARGET:
		case GameMessage.DRAW:
		case GameMessage.DAMAGE:
		case GameMessage.RECOVER:
		case GameMessage.LP_UPDATE:
		case GameMessage.PAY_LP_COST:
		case GameMessage.EQUIP:
		case GameMessage.UNEQUIP:
		case GameMessage.CARD_TARGET:
		case GameMessage.CANCEL_TARGET:
		case GameMessage.ADD_COUNTER:
		case GameMessage.REMOVE_COUNTER:
		case GameMessage.ATTACK:
		case GameMessage.BATTLE:
		case GameMessage.ATTACK_DISABLED:
		case GameMessage.DAMAGE_STEP_START:
		case GameMessage.DAMAGE_STEP_END:
		case GameMessage.MISSED_EFFECT:
		case GameMessage.TOSS_COIN:
		case GameMessage.TOSS_DICE:
		case GameMessage.ANNOUNCE_RACE:
		case GameMessage.ANNOUNCE_ATTRIB:
		case GameMessage.ANNOUNCE_CARD:
		case GameMessage.ANNOUNCE_NUMBER:
		case GameMessage.CARD_HINT:
		case GameMessage.MATCH_KILL:
		case GameMessage.TAG_SWAP:
			break;

		default:
			break;
		}
		return 0;
	}

}
