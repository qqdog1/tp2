package name.qd.tp2.exchanges.BTSE;

import name.qd.tp2.exchanges.AbstractExchange;

public class BTSEExchange extends AbstractExchange {
	@Override
	public String getExchangeName() {
		return "BTSE";
	}
	
	@Override
	public boolean subscribe(String symbol) {
		return false;
	}

	@Override
	public boolean unsubscribe(String symbol) {
		return false;
	}

	@Override
	public String sendOrder(String name, String symbol) {
		return null;
	}

	@Override
	public boolean cancelOrder(String name, String orderId) {
		return false;
	}

	@Override
	public String getBalance(String name, String symbol) {
		return null;
	}
}
