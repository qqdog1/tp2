package name.qd.tp2.exchanges.BTSE;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
	private static String REST_URL = "https://api.btse.com/futures/";
	private static String WS_URL = "wss://ws.btse.com/ws/futures";
	
	private Logger log = LoggerFactory.getLogger(BTSEFuturesExchange.class);
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private ChannelMessageHandler channelMessageHandler = new BTSEChannelMessageHandler();
	private boolean isWebSocketConnect = false;
	
	private MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
	
	public BTSEFuturesExchange() {
		super(REST_URL, WS_URL);
		
		executor.execute(channelMessageHandler);
		if(webSocket == null) initWebSocket(new BTSEWebSocketListener());
	}
	
	private Request.Builder createRequestBuilder(String path) {
		HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
		urlBuilder.addPathSegments(path);
		return new Request.Builder().url(urlBuilder.build().url().toString());
	}
	
	@Override
	public boolean isReady() {
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
		return sendOrder(userName, objectNode);
	}
	
	@Override
	public String sendMarketOrder(String userName, String strategyName, String symbol, BuySell buySell, double qty) {
		ObjectNode objectNode = JsonUtils.objectMapper.createObjectNode();
		objectNode.put("side", buySell.name());
		objectNode.put("size", qty);
		objectNode.put("symbol", symbol);
		objectNode.put("type", "MARKET");
		return sendOrder(userName, objectNode);
	}
	
	private String sendOrder(String userName, ObjectNode objectNode) {
		ApiKeySecret apiKeySecret = getUserApiKeySecret(userName);
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
				if(orderStatus == 2) {
					orderId = ackNode.get("orderID").asText();
				} else {
					log.error("Order failed: {}", responseString);
				}
			}
		} catch (IOException e) {
			log.error("send order failed. {}", responseString, e);
		}
		
		return orderId;
	}

	@Override
	public boolean cancelOrder(String userName, String strategyName, String symbol, String orderId) {
		ApiKeySecret apiKeySecret = getUserApiKeySecret(userName);
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
		ApiKeySecret apiKeySecret = getUserApiKeySecret(userName);
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
	
	private String getSign(String userName, String path, String nonce, String data) throws UnsupportedEncodingException {
		String raw = path + nonce + data;
		
		ApiKeySecret apiKeySecret = getUserApiKeySecret(userName);
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
	
	private class BTSEChannelMessageHandler extends ChannelMessageHandler {
		@Override
		public void processMessage(String text) {
			try {
				JsonNode node = JsonUtils.objectMapper.readTree(text);
				if(node.has("topic")) {
					String messageType = node.get("topic").asText();
					if("orderBook".equals(messageType)) {
						processOrderbook(node);
					}
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
