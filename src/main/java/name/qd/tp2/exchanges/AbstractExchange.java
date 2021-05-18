package name.qd.tp2.exchanges;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import name.qd.tp2.exchanges.cache.FillCache;
import name.qd.tp2.exchanges.cache.OrderbookCache;
import name.qd.tp2.exchanges.vo.ApiKeySecret;
import name.qd.tp2.exchanges.vo.Orderbook;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public abstract class AbstractExchange implements Exchange {
	// okHttp
	private String restUrl;
	private String wsUrl;
	private final OkHttpClient okHttpClient = new OkHttpClient.Builder().pingInterval(10, TimeUnit.SECONDS).build();
	protected HttpUrl httpUrl;
	protected WebSocket webSocket;
	
	// user cache
	private Map<String, ApiKeySecret> mapKeySecret = new HashMap<>();
	private OrderbookCache orderbookCache = new OrderbookCache();
	private FillCache fillCache = new FillCache();
	
	public AbstractExchange(String restUrl, String wsUrl) {
		this.restUrl = restUrl;
		this.wsUrl = wsUrl;
		
		if(restUrl != null) {
			httpUrl = HttpUrl.parse(restUrl);
		}
	}
	
	public void initWebSocket(WebSocketListener webSocketListener) {
		webSocket = createWebSocket(wsUrl, webSocketListener);
	}
	
	public void addUser(String userName, ApiKeySecret apiKeySecret) {
		mapKeySecret.put(userName, apiKeySecret);
	}
	
	public ApiKeySecret getUserApiKeySecret(String userName) {
		return mapKeySecret.get(userName);
	}
	
	@Override
	public Orderbook getOrderbook(String symbol) {
		return orderbookCache.getOrderbook(symbol);
	}
	
	protected void updateOrderbook(String symbol, Orderbook orderbook) {
		orderbookCache.updateOrderbook(symbol, orderbook);
	}
	
	protected WebSocket createWebSocket(String url, WebSocketListener listener) {
		Request request = new Request.Builder().url(url).build();
		return okHttpClient.newWebSocket(request, listener);
	}
	
	protected String sendGetRequest(Request request) throws IOException {
		Response response = okHttpClient.newCall(request).execute();
		return response.body().string();
	}
}
