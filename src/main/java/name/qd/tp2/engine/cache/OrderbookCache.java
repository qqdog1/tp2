package name.qd.tp2.engine.cache;

import java.util.HashMap;
import java.util.Map;

import name.qd.tp2.engine.cache.vo.Orderbook;

public class OrderbookCache {
	private static OrderbookCache instance = new OrderbookCache();
	
	private Map<String, Map<String, Orderbook>> map = new HashMap<>();
	
	private OrderbookCache() {
	}
	
	public static OrderbookCache getInstance() {
		return instance;
	}
	
	public void updateOrderbook(String exchange, String symbol, Orderbook orderbook) {
		if(!map.containsKey(exchange)) {
			map.put(exchange, new HashMap<>());
		}
		map.get(exchange).put(symbol, orderbook);
	}
	
	public Orderbook getOrderbook(String exchange, String symbol) {
		Map<String, Orderbook> mapExchange = map.get(exchange);
		if(mapExchange != null) {
			Orderbook orderbook = mapExchange.get(symbol);
			if(orderbook != null) {
				return orderbook;
			}
		}
		return null;
	}
}
