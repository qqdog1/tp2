package name.qd.tp2.exchanges.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import name.qd.tp2.exchanges.vo.Fill;

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
		Map<String, Queue<Fill>> userQueue = map.get(exchange);
	}
}
