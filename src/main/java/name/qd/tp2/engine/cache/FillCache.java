package name.qd.tp2.engine.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import name.qd.tp2.engine.cache.vo.Fill;

public class FillCache {
	private static FillCache instance = new FillCache();
	private Map<String, Map<String, Queue<Fill>>> map = new HashMap<>();

	public static FillCache getInstance() {
		return instance;
	}
	
	public void putFill(String exchange, String name, Fill fill) {
		if(!map.containsKey(exchange)) {
			map.put(exchange, new HashMap<>());
		}
		
	}
}
