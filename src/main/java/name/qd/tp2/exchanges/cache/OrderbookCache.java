package name.qd.tp2.exchanges.cache;

import java.util.HashMap;
import java.util.Map;

import name.qd.tp2.exchanges.vo.Orderbook;

public class OrderbookCache {
	private Map<String, Orderbook> map = new HashMap<>();
	
	public OrderbookCache() {
	}
	
	public void updateOrderbook(String symbol, Orderbook orderbook) {
		map.put(symbol, orderbook);
	}
	
	public Orderbook getOrderbook(String symbol) {
		return map.get(symbol);
	}
}
