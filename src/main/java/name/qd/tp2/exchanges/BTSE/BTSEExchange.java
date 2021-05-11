package name.qd.tp2.exchanges.BTSE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.qd.tp2.exchanges.AbstractExchange;
import name.qd.tp2.exchanges.Exchange;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class BTSEExchange extends AbstractExchange {
	private Logger log = LoggerFactory.getLogger(BTSEExchange.class);
	private WebSocket webSocket;
	private boolean isWebSocketConnect = false;
	
	@Override
	public String getExchangeName() {
		return "BTSE";
	}
	
	@Override
	public void subscribe(String symbol) {
		if(webSocket == null) initWebSocket();
		while (!isWebSocketConnect) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		webSocket.send("{\"op\":\"subscribe\",\"args\":[\"orderBook:" + symbol + "\"]");
	}

	@Override
	public void unsubscribe(String symbol) {
	}

	@Override
	public String sendOrder(String userName, String strategyName, String symbol, double price, double qty) {
		return null;
	}

	@Override
	public boolean cancelOrder(String userName, String strategyName, String orderId) {
		return false;
	}

	@Override
	public String getBalance(String name, String symbol) {
		return null;
	}
	
	private void initWebSocket() {
		webSocket = createWebSocket("wss://ws.btse.com/ws/futures", new BTSEWebSocketListener());
	}
	
	public class BTSEWebSocketListener extends WebSocketListener {
		@Override
		public void onOpen(WebSocket socket, Response response) {
			System.out.println("open");
			isWebSocketConnect = true;
			log.info("BTSE websocket opened.");
		}
		
		@Override
		public void onMessage(WebSocket webSocket, String text) {
			System.out.println(text);
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
	
	public static void main(String[] s) {
		Exchange exchange = new BTSEExchange();
		exchange.subscribe("ETHPFC_0");
	}
}
