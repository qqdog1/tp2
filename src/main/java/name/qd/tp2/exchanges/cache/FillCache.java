package name.qd.tp2.exchanges.cache;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import name.qd.tp2.exchanges.vo.Fill;

public class FillCache {
	// strategy name, Fills
	private Map<String, LinkedList<Fill>> map = new HashMap<>();

	public FillCache() {
	}
	
	public void offerFill(String strategyName, Fill fill) {
		if(!map.containsKey(strategyName)) {
			map.put(strategyName, new LinkedList<Fill>());
		}
		map.get(strategyName).offer(fill);
	}
	
	public Fill popFill(String strategyName) {
		if(map.containsKey(strategyName) && map.get(strategyName).size() > 0) {
			return map.get(strategyName).pop();
		}
		return null;
	}
}
