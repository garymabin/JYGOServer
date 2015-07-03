package cn.garymb.ygoserver.ygo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import cn.garymb.ygoserver.ygo.ocgwrapper.Card;
import cn.garymb.ygoserver.ygo.ocgwrapper.CardsManager;
import cn.garymb.ygoserver.ygo.ocgwrapper.type.CardType;

public class Deck {
	public List<Integer> main;
	public List<Integer> extra;
	public List<Integer> side;
	
	public Deck() {
		main = new ArrayList<Integer>(60);
		extra = new ArrayList<Integer>(15);
		side = new ArrayList<Integer>(15);
	}
	
	public void addMain(int id) {
		Card card = CardsManager.peekInstance().getCard(id);
		if (card == null) {
			return;
		}
		if ((card.type & CardType.Token.intValue()) != 0)
            return;
        if ((card.type & 0x802040) != 0)
        {
            if (extra.size() < 15)
            	extra.add(id);
        }
        else
        {
            if (main.size() < 60)
            	main.add(id);
        }
	}
	
	public void addSide(int id) {
        Card card = CardsManager.peekInstance().getCard(id);
        if (card == null)
            return;
        if ((card.type & CardType.Token.intValue()) != 0)
            return;
        if (side.size() < 15)
        	side.add(id);
	}
	
	public int check(Banlist ban, boolean ocg, boolean tcg) {
		if (main.size() < 40 || main.size() > 60 || extra.size() > 15 || side.size() > 15) {
			return 1;
		}
		@SuppressWarnings("unchecked")
		List<Integer>[] stacks = (List<Integer>[]) new List<?>[]{main, extra, side};
		Map<Integer, Integer> cards = new HashMap<Integer, Integer>();
		for (List<Integer> stack : stacks) {
			for (int tmpid : stack) {
				Card card = CardsManager.peekInstance().getCard(tmpid);
				int id = card.getId();
				if (card.alias != 0) {
					id = card.alias;
				}
				if (cards.containsKey(id)) {
					cards.put(id,  cards.get(id).intValue() + 1);
				} else {
					cards.put(id, 1);
				}
				if (!ocg && card.getOt() == 1 || !tcg && card.getOt()  == 2) {
					return tmpid;
				}
			}
		}
		if (ban == null) {
			return 0;
		}
		for (Entry<Integer, Integer> entry : cards.entrySet()) {
			int max = ban.getQuantity(entry.getKey());
			if (entry.getValue() > max) {
				return entry.getKey();
			}
		}
		return 0;
	}
	
	@SuppressWarnings("unchecked")
	public boolean check(Deck deck) {
		if (deck.main.size() != this.main.size() || deck.extra.size() != this.extra.size()) {
			return false;
		}
		Map<Integer, Integer> cards = new HashMap<Integer, Integer>();
		Map<Integer, Integer> ncards = new HashMap<Integer, Integer>();
		
		List<Integer>[] stacks = (List<Integer>[]) new List<?>[]{this.main, this.extra, this.side};
		for (List<Integer> stack : stacks) {
			for (int id : stack) {
				if (!cards.containsKey(id)) {
					cards.put(id, 1);
				} else {
					cards.put(id, cards.get(id) + 1);
				}
			}
		}
		stacks = (List<Integer>[]) new List<?>[]{deck.main, deck.extra, deck.side};
		for (List<Integer> stack : stacks) {
			for (int id : stack) {
				if (!ncards.containsKey(id)) {
					ncards.put(id, 1);
				} else {
					ncards.put(id, ncards.get(id) + 1);
				}
			}
		}
		for (Entry<Integer, Integer> entry : cards.entrySet()) {
			if (!ncards.containsKey(entry.getKey())) {
				return false;
			}
			if (ncards.get(entry.getKey()) != entry.getValue()) {
				return false;
			}
		}
		return true;
		
	}

}
