package name.qd.tp2.exchanges;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.qd.tp2.exchanges.BTSE.BTSEExchange;
import name.qd.tp2.exchanges.vo.Orderbook;

public class ExchangeManager {
	private Logger log = LoggerFactory.getLogger(ExchangeManager.class);
	
	public static final String BTSE_EXCHANGE_NAME = "BTSE";
	
	private Map<String, Exchange> mapExchange = new HashMap<>();
	
	private static ExchangeManager instance = new ExchangeManager();
	
	private ExchangeManager() {
	}
	
	public static ExchangeManager getInstance() {
		return instance;
	}
	
	public void initExchange(String exchange) {
		if(!mapExchange.containsKey(exchange)) {
			if(BTSE_EXCHANGE_NAME.equals(exchange)) {
				mapExchange.put(BTSE_EXCHANGE_NAME, new BTSEExchange());
			} else {
				log.error("exchange not implement yet, {}", exchange);
			}
		}
	}
	
	public boolean isExchangeReady(String exchange) {
		return mapExchange.get(exchange).isReady();
	}
	
	public void subscribe(String exchange, String symbol) {
		try {
			mapExchange.get(exchange).subscribe(symbol);
		} catch (NullPointerException e) {
			log.error("exchange not exist, {}", exchange);
		}
	}
	
	public void unsubscribe(String exchange, String symbol) {
		try {
			mapExchange.get(exchange).unsubscribe(symbol);
		} catch (NullPointerException e) {
			log.error("exchange not exist, {}", exchange);
		}
	}
	
	public Orderbook getOrderbook(String exchange, String symbol) {
		try {
			return mapExchange.get(exchange).getOrderbook(symbol).clone();
		} catch (NullPointerException e) {
			log.error("exchange not exist, {}", exchange);
		} catch (CloneNotSupportedException e) {
			log.error("error occur when cloning orderbook, exchange:{}, symbol:{}", exchange, symbol, e);
		}
		return null;
	}
	
	public String sendOrder(String exchange, String userName, String symbol, double price, double qty) {
		// order id
		return null;
	}
}
