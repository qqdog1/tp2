package name.qd.tp2.exchanges;

public interface Exchange {
	public String getExchangeName();
	public void addAccount(String name, String apiKey, String secret);
	
	public boolean subscribe(String symbol);
	public boolean unsubscribe(String symbol);
	
	public String sendOrder(String name, String symbol);
	public boolean cancelOrder(String name, String orderId);
	
	public String getBalance(String name, String symbol);
}
