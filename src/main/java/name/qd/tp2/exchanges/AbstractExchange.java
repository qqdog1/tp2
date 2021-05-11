package name.qd.tp2.exchanges;

import java.util.HashMap;
import java.util.Map;

import name.qd.tp2.engine.cache.OrderbookCache;
import name.qd.tp2.engine.cache.vo.Orderbook;
import name.qd.tp2.exchanges.vo.ApiKeySecret;

public abstract class AbstractExchange implements Exchange {
	public static String name;
	private Map<String, ApiKeySecret> mapKeySecret = new HashMap<>();
	
	public void addAccount(String name, String apiKey, String secret) {
		mapKeySecret.put(name, new ApiKeySecret(apiKey, secret));
	}
	
	public void updateOrderbook(String symbol, Orderbook orderbook) {
		OrderbookCache.getInstance().updateOrderbook(getExchangeName(), symbol, orderbook);
	}
}
