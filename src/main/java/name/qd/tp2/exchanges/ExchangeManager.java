package name.qd.tp2.exchanges;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.qd.tp2.constants.BuySell;
import name.qd.tp2.exchanges.BTSE.BTSEFuturesExchange;
import name.qd.tp2.exchanges.Fake.FakeExchange;
import name.qd.tp2.exchanges.vo.ApiKeySecret;
import name.qd.tp2.exchanges.vo.Fill;
import name.qd.tp2.exchanges.vo.Orderbook;

public class ExchangeManager {
	private Logger log = LoggerFactory.getLogger(ExchangeManager.class);
	
	public static final String BTSE_EXCHANGE = "BTSE";
	public static final String FAKE_EXCHANGE = "Fake";
	
	private Map<String, Exchange> mapExchange = new HashMap<>();
	
	private static ExchangeManager instance = new ExchangeManager();
	
	private ExchangeManager() {
	}
	
	public static ExchangeManager getInstance() {
		return instance;
	}
	
	public void initExchange(String exchange, String ... customize) {
		if(!mapExchange.containsKey(exchange)) {
			if(BTSE_EXCHANGE.equals(exchange)) {
				mapExchange.put(BTSE_EXCHANGE, 
						// TODO 看怎麼修 上面的param不要傳array 下面不要抓index
						new BTSEFuturesExchange(ExchangeSettings.getExchangeRestUrlByEnv(exchange, customize[0]),
												ExchangeSettings.getExchangeWSUrlByEnv(exchange, customize[0]),
												customize[1]));
			} else if(FAKE_EXCHANGE.equals(exchange)) {
				mapExchange.put(FAKE_EXCHANGE, new FakeExchange("", ""));
			} else {
				log.error("exchange not implement yet, {}", exchange);
			}
		}
	}
	
	public boolean isExchangeReady(String exchange) {
		return mapExchange.get(exchange).isReady();
	}
	
	public void addUserApiKeySecret(String exchange, String userName, ApiKeySecret apiKeySecret) {
		mapExchange.get(exchange).addUser(userName, apiKeySecret);
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
	
	public Map<String, Double> getBalance(String exchange, String userName) {
		return mapExchange.get(exchange).getBalance(userName);
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
	
	public List<Fill> getFill(String strategyName, String exchange) {
		return mapExchange.get(exchange).getFill(strategyName);
	}
	
	public String sendOrder(String strategyName, String exchange, String userName, String symbol, BuySell buySell, double price, double qty) {
		if(price == 0) {
			return mapExchange.get(exchange).sendMarketOrder(userName, strategyName, symbol, buySell, qty);
		} else {
			return mapExchange.get(exchange).sendLimitOrder(userName, strategyName, symbol, buySell, price, qty);
		}
	}
	
	public boolean cancelOrder(String strategyName, String exchange, String userName, String symbol, String orderId) {
		return mapExchange.get(exchange).cancelOrder(userName, strategyName, symbol, orderId);
	}
}
