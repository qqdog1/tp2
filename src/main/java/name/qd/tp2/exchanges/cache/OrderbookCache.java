package name.qd.tp2.exchanges.cache;

import java.util.HashMap;
import java.util.Map;

import name.qd.tp2.exchanges.vo.Orderbook;

public class OrderbookCache {
	// symbol, orderbook
	private Map<String, Orderbook> map = new HashMap<>();
	
	public OrderbookCache() {
	}
	
	// 一次更新整個 要有delta的
	public void updateOrderbook(String symbol, Orderbook orderbook) {
		map.put(symbol, orderbook);
	}
	
	public Orderbook getOrderbook(String symbol) {
		return map.get(symbol);
	}
}
