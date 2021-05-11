package name.qd.tp2.exchanges;

import java.util.HashMap;
import java.util.Map;

import name.qd.tp2.exchanges.cache.OrderbookCache;
import name.qd.tp2.exchanges.vo.ApiKeySecret;
import name.qd.tp2.exchanges.vo.Orderbook;

public abstract class AbstractExchange implements Exchange {
	private Map<String, ApiKeySecret> mapKeySecret = new HashMap<>();
	private OrderbookCache orderbookCache = new OrderbookCache();
	
	public void addAccount(String name, String apiKey, String secret) {
		mapKeySecret.put(name, new ApiKeySecret(apiKey, secret));
	}
	
	public void updateOrderbook(String symbol, Orderbook orderbook) {
		orderbookCache.updateOrderbook(symbol, orderbook);
	}
}
