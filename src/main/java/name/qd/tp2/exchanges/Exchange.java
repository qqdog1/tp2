package name.qd.tp2.exchanges;

import java.util.Map;

import name.qd.tp2.exchanges.vo.ApiKeySecret;
import name.qd.tp2.exchanges.vo.Orderbook;

public interface Exchange {
	public boolean isReady();
	
	public void addUser(String userName, ApiKeySecret apiKeySecret);
	
	public void subscribe(String symbol);
	public void unsubscribe(String symbol);
	public Orderbook getOrderbook(String symbol);
	
	public String sendOrder(String userName, String strategyName, String symbol, double price, double qty);
	public boolean cancelOrder(String userName, String strategyName, String orderId);
	
	public Map<String, Double> getBalance(String userName);
	public String getBalance(String userName, String symbol);
}
