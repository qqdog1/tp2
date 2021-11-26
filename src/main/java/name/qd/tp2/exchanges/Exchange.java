package name.qd.tp2.exchanges;

import java.util.List;
import java.util.Map;

import name.qd.tp2.constants.BuySell;
import name.qd.tp2.exchanges.vo.ApiKeySecret;
import name.qd.tp2.exchanges.vo.Fill;
import name.qd.tp2.exchanges.vo.Orderbook;

public interface Exchange {
	public boolean isReady();
	
	public void addUser(String userName, ApiKeySecret apiKeySecret);
	
	public void subscribe(String symbol);
	public void unsubscribe(String symbol);
	public Orderbook getOrderbook(String symbol);
	public List<Fill> getFill(String strategyName);
	
	public String sendLimitOrder(String userName, String strategyName, String symbol, BuySell buySell, double price, double qty);
	public String sendMarketOrder(String userName, String strategyName, String symbol, BuySell buySell, double qty);
	public boolean cancelOrder(String userName, String strategyName, String symbol, String orderId);
	
	public Map<String, Double> getBalance(String userName);
	public String getBalance(String userName, String symbol);
}
