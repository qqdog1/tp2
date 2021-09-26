package name.qd.tp2.exchanges.Fake;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.qd.tp2.constants.BuySell;
import name.qd.tp2.exchanges.AbstractExchange;
import name.qd.tp2.exchanges.vo.Fill;
import name.qd.tp2.exchanges.vo.Orderbook;

public class FakeExchange extends AbstractExchange {
	private Logger log = LoggerFactory.getLogger(FakeExchange.class);
	private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
	
	private Set<String> setSubscribedSymbol = new HashSet<>();
	// orderId, strategyName
	private Map<String, String> mapOrderIdToStrategy = new HashMap<>();
		
	// symbol, price
	private Map<String, PriceSimulator> mapSymbolPrice = new HashMap<>();

	// symbol, orderId, order
	private Map<String, Map<String, Order>> mapOrders = new ConcurrentHashMap<>();

	public FakeExchange(String restUrl, String wsUrl) {
		super(restUrl, wsUrl);
		
		init();
		
		scheduledExecutorService.scheduleAtFixedRate(new FakeExchangeRunner(), 0, 1, TimeUnit.SECONDS);
	}

	private void init() {
		String[] states = new String[] {StateController.UP, StateController.DOWN, StateController.UP, StateController.DOWN};
		int[] times = new int[] {300, 200, 200, 300};
		mapSymbolPrice.put("ETHPFC", new PriceSimulator(2900, states, times));
	}
	
	@Override
	public boolean isReady() {
		return true;
	}

	@Override
	public void subscribe(String symbol) {
		synchronized(setSubscribedSymbol) {
			setSubscribedSymbol.add(symbol);
		}
	}

	@Override
	public void unsubscribe(String symbol) {
	}

	@Override
	public List<Fill> getFillHistory(String username, String symbol, long startTime, long endTime) {
		return null;
	}

	@Override
	public String sendLimitOrder(String userName, String strategyName, String symbol, BuySell buySell, double price, double qty) {
		UUID uuid = UUID.randomUUID();
		mapOrderIdToStrategy.put(uuid.toString(), strategyName);
		if(!mapOrders.containsKey(symbol)) {
			mapOrders.put(symbol, new ConcurrentHashMap<>());
		}
		Order order = new Order(buySell, price, qty);
		mapOrders.get(symbol).put(uuid.toString(), order);
		return uuid.toString();
	}

	@Override
	public boolean cancelOrder(String userName, String strategyName, String symbol, String orderId) {
		if(!mapOrders.containsKey(symbol)) return false;
		if(mapOrders.get(symbol).remove(orderId) != null) {
			return mapOrderIdToStrategy.remove(orderId) != null;
		}
		return false;
	}
	
	@Override
	public String sendMarketOrder(String userName, String strategyName, String symbol, BuySell buySell, double qty) {
		return null;
	}

	@Override
	public Map<String, Double> getBalance(String userName) {
		return null;
	}

	@Override
	public String getBalance(String userName, String symbol) {
		return null;
	}
	
	// 假交易所核心
	private class FakeExchangeRunner implements Runnable {
		@Override
		public void run() {
			// orderbook
			for(String symbol : mapSymbolPrice.keySet()) {
				double price = mapSymbolPrice.get(symbol).next();
				Orderbook orderbook = new Orderbook();
				orderbook.addAsk(price + 1, 999);
				orderbook.addBid(price - 1, 999);
				updateOrderbook(symbol, orderbook);
			}
			
			// fill
		}
	}
}
