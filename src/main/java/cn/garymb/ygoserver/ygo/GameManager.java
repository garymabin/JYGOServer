package cn.garymb.ygoserver.ygo;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import cn.garymb.ygoserver.server.AbstractMessageReceiver;
import cn.garymb.ygoserver.util.TimerTask;

public class GameManager {
	
	private static final Logger log = Logger.getLogger(GameManager.class.getName());
	
	private static ConcurrentHashMap<String, GameRoom> sRooms = new ConcurrentHashMap<String, GameRoom>();
	
	private static AbstractMessageReceiver sAsyncEventProcessor;
	
	public static GameRoom createOrGetGame(GameConfig config) {
		if (sRooms.containsKey(config.name)) {
			return sRooms.get(config.name);
		}
		return createRoom(config);
	}
	
	public static GameRoom getRoom(String name) {
		if (sRooms.containsKey(name)) {
			return sRooms.get(name);
		}
		return null;
	}

	public static GameRoom createRoom(GameConfig config) {
        GameRoom room = new GameRoom(config);
        sRooms.put(config.name, room);
        if (log.isLoggable(Level.INFO)) {
        	log.log(Level.INFO, "new game created: {0}", new Object[]{config.name});
        }
        return room;
    }
	
	public static boolean isGameExist(String name) {
		return sRooms.containsKey(name);
	}
	
	public static boolean isGameExist(GameConfig config) {
		return sRooms.containsKey(config.name);
	}
	
	public static String randomGameName() {
		return UUID.randomUUID().toString();
	}

	public static void setGameAsyncEventProcessor(AbstractMessageReceiver amr) {
		sAsyncEventProcessor = amr;
 	}

	public static void submit(TimerTask task, long delayInMillis) {
		sAsyncEventProcessor.addTimerTask(task, delayInMillis);
	}
}
