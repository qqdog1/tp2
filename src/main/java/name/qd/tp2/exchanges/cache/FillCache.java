package name.qd.tp2.exchanges.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.qd.tp2.exchanges.vo.Fill;

public class FillCache {
	// strategy name, Fills
	private Map<String, ArrayList<Fill>> map = new HashMap<>();

	public FillCache() {
	}
	
	public void addFill(String strategyName, Fill fill) {
		if(!map.containsKey(strategyName)) {
			map.put(strategyName, new ArrayList<Fill>());
		}
		map.get(strategyName).add(fill);
	}
	
	public List<Fill> getFill(String strategyName) {
		if(!map.containsKey(strategyName)) return Collections.emptyList();
		List<Fill> lst = new ArrayList<>(map.get(strategyName));
		map.get(strategyName).clear();
		return lst;
	}
	
}
