package name.qd.tp2.exchanges.MAX;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import name.qd.tp2.constants.BuySell;
import name.qd.tp2.exchanges.AbstractExchange;
import name.qd.tp2.exchanges.ChannelMessageHandler;
import name.qd.tp2.utils.JsonUtils;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class MAXExchange extends AbstractExchange {
	private Logger log = LoggerFactory.getLogger(MAXExchange.class);
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private ChannelMessageHandler channelMessageHandler = new MAXChannelMessageHandler();
	private WebSocketListener webSocketListener = new MAXWebSocketListener();
	private boolean isWebSocketConnect = false;
	
	private Set<String> setSubscribedSymbol = new HashSet<>();
	
	public MAXExchange(String restUrl, String wsUrl) {
		super(restUrl, wsUrl);
		
		executor.execute(channelMessageHandler);
		if (webSocket == null)
			initWebSocket(webSocketListener);
	}

	@Override
	public boolean isReady() {
		// TODO 先這樣
		return isWebSocketConnect;
	}

	@Override
	public void subscribe(String symbol) {
		setSubscribedSymbol.add(symbol);
		ObjectNode objectNode = JsonUtils.objectMapper.createObjectNode();
		objectNode.put("action", "sub");
		objectNode.put("id", "tp2");
		ArrayNode arrayNode = objectNode.putArray("subscriptions");
		ObjectNode subNode = JsonUtils.objectMapper.createObjectNode();
		subNode.put("channel", "book");
		subNode.put("market", symbol);
		subNode.put("depth", 1);
		arrayNode.add(subNode);
		webSocket.send(objectNode.toString());
	}

	@Override
	public void unsubscribe(String symbol) {
		
	}

	@Override
	public String sendLimitOrder(String userName, String strategyName, String symbol, BuySell buySell, double price, double qty) {
		return null;
	}

	@Override
	public String sendMarketOrder(String userName, String strategyName, String symbol, BuySell buySell, double qty) {
		return null;
	}

	@Override
	public boolean cancelOrder(String userName, String strategyName, String symbol, String orderId) {
		return false;
	}

	@Override
	public Map<String, Double> getBalance(String userName) {
		return null;
	}

	@Override
	public String getBalance(String userName, String symbol) {
		return null;
	}
	
	private void reconnect() {
		initWebSocket(webSocketListener);
	}
	
	private class MAXWebSocketListener extends WebSocketListener {
		@Override
		public void onOpen(WebSocket socket, Response response) {
			isWebSocketConnect = true;
			log.info("MAX websocket opened.");
		}

		@Override
		public void onMessage(WebSocket webSocket, String text) {
			channelMessageHandler.onMessage(text);
		}

		@Override
		public void onClosed(WebSocket webSocket, int code, String reason) {
			isWebSocketConnect = false;
			log.info("MAX websocket closed. {}", reason);
			reconnect();
		}

		@Override
		public void onFailure(WebSocket webSocket, Throwable t, Response response) {
			isWebSocketConnect = false;
			log.error("MAX websocket failure.", t);
			reconnect();
		}
	}
	
	private class MAXChannelMessageHandler extends ChannelMessageHandler {
		@Override
		public void processMessage(String text) {
			System.out.println(text);
		}
	}
}
