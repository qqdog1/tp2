package name.qd.tp2.exchanges.BTSE;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import name.qd.tp2.constants.BuySell;
import name.qd.tp2.exchanges.AbstractExchange;
import name.qd.tp2.exchanges.ChannelMessageHandler;
import name.qd.tp2.exchanges.Exchange;
import name.qd.tp2.exchanges.vo.ApiKeySecret;
import name.qd.tp2.exchanges.vo.Fill;
import name.qd.tp2.exchanges.vo.Orderbook;
import name.qd.tp2.utils.JsonUtils;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class BTSEFuturesExchange extends AbstractExchange {
//	private static String REST_URL = "https://api.btse.com/futures/";
//	private static String WS_URL = "wss://ws.btse.com/ws/futures";
	private static String REST_URL = "https://testapi.btse.io/futures";
	private static String WS_URL = "wss://testws.btse.io/ws/futures";
	
	private Logger log = LoggerFactory.getLogger(BTSEFuturesExchange.class);
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
	private ChannelMessageHandler channelMessageHandler = new BTSEChannelMessageHandler();
	private boolean isWebSocketConnect = false;
	
	// orderId, strategyName
	private Map<String, String> mapOrderIdToStrategy = new HashMap<>();
	// orderId, userName
	private Map<String, String> mapOrderIdToUserName = new HashMap<>();
	// unknown fill
	private Map<Integer, List<Fill>> mapUnknownFill = new HashMap<>();
	
	private MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
	
	public BTSEFuturesExchange() {
		super(REST_URL, WS_URL);
		
		executor.execute(channelMessageHandler);
		scheduledExecutorService.scheduleAtFixedRate(new BTSERunner(), 0, 1, TimeUnit.SECONDS);
		if(webSocket == null) initWebSocket(new BTSEWebSocketListener());
	}
	
	public void addUser(String userName, ApiKeySecret apiKeySecret) {
		// TODO websocket 這樣一個user要一個connection
		super.addUser(userName, apiKeySecret);
		websocketLogin(userName);
		subscribeFill(userName);
	}
	
	private void websocketLogin(String userName) {
		ApiKeySecret apiKeySecret = getApiKeySecret(userName);
		String nonce = String.valueOf(System.currentTimeMillis());
		String sign = null;
		try {
			sign = getSign(userName, "/futures/api/topic", nonce, "");
		} catch (UnsupportedEncodingException e) {
			log.error("get sign failed.", e);
		}
		
		ObjectNode objectNode = JsonUtils.objectMapper.createObjectNode();
		objectNode.put("op", "login");
		ArrayNode arrayNode = objectNode.putArray("args");
		arrayNode.add(apiKeySecret.getApiKey());
		arrayNode.add(nonce);
		arrayNode.add(sign);
		
		webSocket.send(objectNode.toString());
	}
	
	private void subscribeFill(String userName) {
		ObjectNode objectNode = JsonUtils.objectMapper.createObjectNode();
		objectNode.put("op", "subscribe");
		ArrayNode arrayNode = objectNode.putArray("args");
		arrayNode.add("notificationApiV2");
		webSocket.send(objectNode.toString());
	}
	
	private Request.Builder createRequestBuilder(String path) {
		HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
		urlBuilder.addPathSegments(path);
		return new Request.Builder().url(urlBuilder.build().url().toString());
	}
	
	@Override
	public boolean isReady() {
		// TODO
		// 這個交易所websocket連上就算ready
		return isWebSocketConnect;
	}
	
	@Override
	public void subscribe(String symbol) {
		ObjectNode objectNode = JsonUtils.objectMapper.createObjectNode();
		objectNode.put("op", "subscribe");
		ArrayNode arrayNode = objectNode.putArray("args");
		arrayNode.add("orderBook:" + symbol + "_0");
		webSocket.send(objectNode.toString());
	}

	@Override
	public void unsubscribe(String symbol) {
		ObjectNode objectNode = JsonUtils.objectMapper.createObjectNode();
		objectNode.put("op", "unsubscribe");
		ArrayNode arrayNode = objectNode.putArray("args");
		arrayNode.add("orderBook:" + symbol + "_0");
		webSocket.send(objectNode.toString());
	}

	@Override
	public String sendLimitOrder(String userName, String strategyName, String symbol, BuySell buySell, double price, double qty) {
		ObjectNode objectNode = JsonUtils.objectMapper.createObjectNode();
		objectNode.put("price", price);
		objectNode.put("side", buySell.name());
		objectNode.put("size", qty);
		objectNode.put("symbol", symbol);
		objectNode.put("type", "LIMIT");
		return sendOrder(strategyName, userName, objectNode);
	}
	
	@Override
	public String sendMarketOrder(String userName, String strategyName, String symbol, BuySell buySell, double qty) {
		ObjectNode objectNode = JsonUtils.objectMapper.createObjectNode();
		objectNode.put("side", buySell.name());
		objectNode.put("size", qty);
		objectNode.put("symbol", symbol);
		objectNode.put("type", "MARKET");
		return sendOrder(strategyName, userName, objectNode);
	}
	
	private String sendOrder(String strategyName, String userName, ObjectNode objectNode) {
		ApiKeySecret apiKeySecret = getApiKeySecret(userName);
		String nonce = String.valueOf(System.currentTimeMillis());
		String sign = null;
		try {
			sign = getSign(userName, "/api/v2.1/order", nonce, objectNode.toString());
		} catch (UnsupportedEncodingException e) {
			log.error("get sign failed when sending order.", e);
		}
		Request.Builder requestBuilderOrder = createRequestBuilder("api/v2.1/order");
		requestBuilderOrder.addHeader("btse-nonce", nonce);
		requestBuilderOrder.addHeader("btse-api", apiKeySecret.getApiKey());
		requestBuilderOrder.addHeader("btse-sign", sign);
		
		RequestBody body = RequestBody.create(objectNode.toString(), MEDIA_TYPE_JSON);
		Request request = requestBuilderOrder.post(body).build();
		
		String responseString = null;
		String orderId = null;
		try {
			responseString = sendRequest(request);
			
			JsonNode node = JsonUtils.objectMapper.readTree(responseString);
			for(JsonNode ackNode : node) {
				int orderStatus = ackNode.get("status").asInt();
				if(orderStatus == 2 || orderStatus == 4 || orderStatus == 5) {
					orderId = ackNode.get("orderID").asText();
				} else {
					log.error("Order failed: {}", responseString);
				}
			}
		} catch (IOException e) {
			log.error("send order failed. {}", responseString, e);
		}
		
		if(orderId != null) {
			mapOrderIdToStrategy.put(orderId, strategyName);
			mapOrderIdToUserName.put(orderId, userName);
		}
		
		return orderId;
	}

	@Override
	public boolean cancelOrder(String userName, String strategyName, String symbol, String orderId) {
		ApiKeySecret apiKeySecret = getApiKeySecret(userName);
		String nonce = String.valueOf(System.currentTimeMillis());
		
		HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
		urlBuilder.addPathSegments("api/v2.1/order");
		urlBuilder.addEncodedQueryParameter("orderID", orderId);
		urlBuilder.addEncodedQueryParameter("symbol", symbol);
		
		Request.Builder requestBuilder = new Request.Builder().url(urlBuilder.build().url().toString());
		String sign = null;
		try {
			sign = getSign(userName, "/api/v2.1/order", nonce, "");
		} catch (UnsupportedEncodingException e) {
			log.error("get sign failed when sending order.", e);
		}
		requestBuilder.addHeader("btse-nonce", nonce);
		requestBuilder.addHeader("btse-api", apiKeySecret.getApiKey());
		requestBuilder.addHeader("btse-sign", sign);
		Request request = requestBuilder.delete().build();
		
		String responseString = null;
		boolean isCancelled = false;
		try {
			responseString = sendRequest(request);
			
			JsonNode node = JsonUtils.objectMapper.readTree(responseString);
			for(JsonNode ackNode : node) {
				int orderStatus = ackNode.get("status").asInt();
				if(orderStatus == 6) {
					isCancelled = true;
				} else {
					log.error("Order failed: {}", responseString);
				}
			}
		} catch (IOException e) {
			log.error("send order failed.", e);
		}
		
		return isCancelled;
	}
	
	@Override
	public Map<String, Double> getBalance(String userName) {
		ApiKeySecret apiKeySecret = getApiKeySecret(userName);
		String nonce = String.valueOf(System.currentTimeMillis());
		String sign = null;
		try {
			sign = getSign(userName, "/api/v2.1/user/wallet", nonce, "");
		} catch (UnsupportedEncodingException e) {
			log.error("get sign failed when query balance.", e);
		}
		
		Request.Builder requestBuilderBalance = createRequestBuilder("api/v2.1/user/wallet");
		requestBuilderBalance.addHeader("btse-nonce", nonce);
		requestBuilderBalance.addHeader("btse-api", apiKeySecret.getApiKey());
		requestBuilderBalance.addHeader("btse-sign", sign);
		Request request = requestBuilderBalance.build();
		String responseString = null;
		try {
			responseString = sendRequest(request);
			
			System.out.println(responseString);
		} catch (IOException e) {
			log.error("query balance failed.", e);
		}
		
		return null;
	}

	@Override
	public String getBalance(String userName, String symbol) {
		
		return null;
	}
	
	private void processOrderbook(JsonNode node) {
		String symbol = node.get("data").get("symbol").asText();
		Orderbook orderbook = new Orderbook();
		JsonNode buyNode = node.get("data").get("buyQuote");
		JsonNode sellNode = node.get("data").get("sellQuote");
		for(JsonNode buy : buyNode) {
			orderbook.addBid(buy.get("price").asDouble(), buy.get("size").asDouble());
		}
		for(JsonNode sell : sellNode) {
			orderbook.addAsk(sell.get("price").asDouble(), sell.get("size").asDouble());
		}
		updateOrderbook(symbol, orderbook);
	}
	
	private void processNotification(JsonNode node) {
		System.out.println(node.toString());
		for(JsonNode notificationNode : node.get("data")) {
			int orderStatus = notificationNode.get("status").asInt();
			if(orderStatus == 4 || orderStatus == 5) {
				processFill(notificationNode);
			}
		}
	}
	
	private void processFill(JsonNode node) {
		String orderId = node.get("orderID").asText();
		String strategyName = mapOrderIdToStrategy.get(orderId);
		String userName = mapOrderIdToUserName.get(orderId);
		
		Fill fill = new Fill();
		fill.setOrderId(orderId);
		fill.setSymbol(node.get("symbol").asText());
		fill.setPrice(node.get("price").asDouble());
		fill.setQty(node.get("fillSize").asDouble());
		
		if(strategyName != null) {
			fill.setUserName(userName);
			addFill(strategyName, fill);
		} else {
			log.warn("Received unknown fill, put to unknown fill cache.");
			if(!mapUnknownFill.containsKey(1)) {
				mapUnknownFill.put(1, new ArrayList<>());
			}
			mapUnknownFill.get(1).add(fill);
		}
	}
	
	private String getSign(String userName, String path, String nonce, String data) throws UnsupportedEncodingException {
		String raw = path + nonce + data;
		
		ApiKeySecret apiKeySecret = getApiKeySecret(userName);
		byte[] hmac_key = apiKeySecret.getSecret().getBytes("UTF-8");
		byte[] hash = HmacUtils.getInitializedMac(HmacAlgorithms.HMAC_SHA_384, hmac_key).doFinal(raw.getBytes());
		return Hex.encodeHexString(hash);
	}
	
	private class BTSEWebSocketListener extends WebSocketListener {
		@Override
		public void onOpen(WebSocket socket, Response response) {
			isWebSocketConnect = true;
			log.info("BTSE websocket opened.");
		}
		
		@Override
		public void onMessage(WebSocket webSocket, String text) {
			channelMessageHandler.onMessage(text);
		}
		
		@Override
		public void onClosed(WebSocket webSocket, int code, String reason) {
			isWebSocketConnect = false;
			log.info("BTSE websocket closed. {}", reason);
		}
		
		@Override
		public void onFailure(WebSocket webSocket, Throwable t, Response response) {
			isWebSocketConnect = false;
			log.error("BTSE websocket failure.", t);
		}
	}
	
	private class BTSERunner implements Runnable {
		@Override
		public void run() {
			for(Integer i = 3 ; i > 0 ; i--) {
				if(mapUnknownFill.containsKey(i)) {
					for(Fill fill : mapUnknownFill.get(i)) {
						String orderId = fill.getOrderId();
						String strategyName = mapOrderIdToStrategy.get(orderId);
						String userName = mapOrderIdToUserName.get(orderId);
						if(strategyName != null) {
							fill.setUserName(userName);
							addFill(strategyName, fill);
						} else {
							if(i != 3) {
								Integer times = i + 1;
								if(!mapUnknownFill.containsKey(times)) {
									mapUnknownFill.put(times, new ArrayList<>());
								}
								mapUnknownFill.get(times).add(fill);
							}
						}
					}
					mapUnknownFill.get(i).clear();
				}
			}
		}
	}
	
	private class BTSEChannelMessageHandler extends ChannelMessageHandler {
		@Override
		public void processMessage(String text) {
			try {
				JsonNode node = JsonUtils.objectMapper.readTree(text);
				if(node.has("topic")) {
					String messageType = node.get("topic").asText();
					if("orderBook".equals(messageType)) {
						processOrderbook(node);
					} else if("notificationApiV2".equals(messageType)) {
						processNotification(node);
					} else {
						log.error("BTSE websocket received unknown message: {}", text);
					}
				} else {
					log.error("BTSE websocket received unknown message: {}", text);
				}
			} catch (JsonProcessingException e) {
				log.error("Parse websocket message to Json format failed. {}", text, e);
			}
		}
	}
	
	public static void main(String[] s) {
		Exchange exchange = new BTSEFuturesExchange();
		exchange.subscribe("ETHPFC");
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		exchange.unsubscribe("ETHPFC");
	}
}
