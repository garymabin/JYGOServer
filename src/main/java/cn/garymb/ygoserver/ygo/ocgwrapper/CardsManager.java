/**
 * CardsManager.java
 * author: mabin
 * 2015年4月7日
 */
package cn.garymb.ygoserver.ygo.ocgwrapper;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import cn.garymb.ygoserver.conf.ConfigManager;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;

public final class CardsManager {
	
	private static final Logger log  = Logger.getLogger(CardsManager.class.getName());
	
	private Map<Integer, Card> cards = new HashMap<Integer, Card>();
	
	private static CardsManager INSTANCE;
	
	private CardsManager() throws SQLiteException {
		SQLiteConnection db = new SQLiteConnection(new File(ConfigManager.peekInstance().getDataBasePath()));
		db.open(true);
		SQLiteStatement st = db.prepare("SELECT id, ot, alias, type, level, race, attribute, atk, def FROM datas");
		try {
			while(st.step()) {
				int id = st.columnInt(0);
				Card c = new Card(id, st.columnInt(1));
				c.alias = st.columnInt(2);
				c.setcode = st.columnInt(3);
				int levelinfo = st.columnInt(4);
				c.level = levelinfo & 0xff;
				c.lscale = (levelinfo >> 24) & 0xff;
				c.rscale = (levelinfo >> 16) & 0xff;
				c.race = st.columnInt(6);
				c.attr = st.columnInt(7);
				c.attack = st.columnInt(8);
				c.defense = st.columnInt(9);
				cards.put(id, c);
			}
		} finally {
			st.dispose();
		}
		db.dispose();
		
	}
	
	public static CardsManager peekInstance() {
		if (INSTANCE == null) {
			try {
				INSTANCE = new CardsManager();
			} catch (SQLiteException e) {
				log.log(Level.SEVERE,"can not load cards database :{0}", e.getMessage());
			}
		}
		return INSTANCE;
	}
}
