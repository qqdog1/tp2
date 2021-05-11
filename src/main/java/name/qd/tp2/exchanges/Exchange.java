package name.qd.tp2.exchanges;

public interface Exchange {
	public String getExchangeName();
	public void addAccount(String userName, String apiKey, String secret);
	
	public void subscribe(String symbol);
	public void unsubscribe(String symbol);
	
	public String sendOrder(String userName, String strategyName, String symbol, double price, double qty);
	public boolean cancelOrder(String userName, String strategyName, String orderId);
	
	public String getBalance(String name, String symbol);
}
