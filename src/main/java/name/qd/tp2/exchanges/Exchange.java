package name.qd.tp2.exchanges;

import name.qd.tp2.exchanges.vo.Orderbook;

public interface Exchange {
	public boolean isReady();
	
	public void addUser(String userName, String apiKey, String secret);
	
	public void subscribe(String symbol);
	public void unsubscribe(String symbol);
	public Orderbook getOrderbook(String symbol);
	
	public String sendOrder(String userName, String strategyName, String symbol, double price, double qty);
	public boolean cancelOrder(String userName, String strategyName, String orderId);
	
	public String getBalance(String userName, String symbol);
}
