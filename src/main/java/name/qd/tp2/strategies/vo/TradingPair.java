package name.qd.tp2.strategies.vo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TradingPair {
	private Map<String, Set<String>> map = new HashMap<>();

	public void addExchange(String exchange) {
		if(!map.containsKey(exchange)) {
			map.put(exchange, new HashSet<>());
		}
	}
	
	public void addSymbol(String exchange, String symbol) {
		addExchange(exchange);
		map.get(exchange).add(symbol);
	}
	
	public Set<String> getAllExchange() {
		return map.keySet();
	}
	
	public Set<String> getAllSymbol(String exchange) {
		return map.get(exchange);
	}
}
