package name.qd.tp2.exchanges;

public class ExchangeManager {
	private static ExchangeManager instance = new ExchangeManager();
	
	private ExchangeManager() {
	}
	
	public static ExchangeManager getInstance() {
		return instance;
	}
	
	public void subscribe(String exchange, String symbol) {
		
	}
	
	public void unsubscribe(String exchange, String symbol) {
		
	}
	
	public String sendOrder(String exchange, String userName, String symbol, double price, double qty) {
		// order id
		return null;
	}
}
