package name.qd.tp2.exchanges;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import name.qd.tp2.exchanges.cache.FillCache;
import name.qd.tp2.exchanges.cache.OrderbookCache;
import name.qd.tp2.exchanges.vo.ApiKeySecret;
import name.qd.tp2.exchanges.vo.Orderbook;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public abstract class AbstractExchange implements Exchange {
	private final OkHttpClient okHttpClient = new OkHttpClient.Builder().pingInterval(10, TimeUnit.SECONDS).build();
	private Map<String, ApiKeySecret> mapKeySecret = new HashMap<>();
	private OrderbookCache orderbookCache = new OrderbookCache();
	private FillCache fillCache = new FillCache();
	
	public void addAccount(String name, String apiKey, String secret) {
		mapKeySecret.put(name, new ApiKeySecret(apiKey, secret));
	}
	
	public void updateOrderbook(String symbol, Orderbook orderbook) {
		orderbookCache.updateOrderbook(symbol, orderbook);
	}
	
	protected WebSocket createWebSocket(String url, WebSocketListener listener) {
		Request request = new Request.Builder().url(url).build();
		return okHttpClient.newWebSocket(request, listener);
	}
}
