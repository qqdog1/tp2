package name.qd.tp2.exchanges.Fake;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.qd.tp2.constants.BuySell;
import name.qd.tp2.exchanges.AbstractExchange;
import name.qd.tp2.exchanges.vo.Fill;
import name.qd.tp2.exchanges.vo.Order;
import name.qd.tp2.exchanges.vo.Orderbook;

public class FakeExchange extends AbstractExchange {
	private Logger log = LoggerFactory.getLogger(FakeExchange.class);
	private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
	
	private Set<String> setSubscribedSymbol = new HashSet<>();
	// orderId, strategyName
	private Map<String, String> mapOrderIdToStrategy = new HashMap<>();
	// orderId, userName
	private Map<String, String> mapOrderIdToUserName = new HashMap<>();
		
	// symbol, price
	private Map<String, PriceSimulator> mapSymbolPrice = new HashMap<>();

	// symbol, orderId, order
	private Map<String, Map<String, Order>> mapOrders = new ConcurrentHashMap<>();

	public FakeExchange(String restUrl, String wsUrl) {
		super(restUrl, wsUrl);
		
		init();
		
		scheduledExecutorService.scheduleWithFixedDelay(new FakeExchangeRunner(), 0, 1, TimeUnit.SECONDS);
	}

	private void init() {
		String[] states = new String[] {StateController.DOWN, StateController.UP, StateController.DOWN, StateController.UP, StateController.DOWN};
		int[] times = new int[] {30, 30, 50, 70, 20};
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
		mapOrderIdToUserName.put(uuid.toString(), userName);
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
			for(String symbol : mapSymbolPrice.keySet()) {
				// orderbook
				double price = mapSymbolPrice.get(symbol).next();
				Orderbook orderbook = new Orderbook();
				orderbook.addAsk(price + 1, 999);
				orderbook.addBid(price - 1, 999);
				updateOrderbook(symbol, orderbook);
				
				// fill
				Map<String, Order> mapSymbolOrders = mapOrders.get(symbol);
				if(mapSymbolOrders == null) return;
				List<String> lstRemoveOrderId = new ArrayList<>();
				for(String orderId : mapSymbolOrders.keySet()) {
					Order order = mapSymbolOrders.get(orderId);
					BuySell buySell = order.getBuySell();
					switch (buySell) {
					case BUY:
						if(order.getPrice() >= price + 1) {
							Fill fill = toFill(order, orderId, symbol);
							lstRemoveOrderId.add(orderId);
							String strategyName = mapOrderIdToStrategy.get(orderId);
							addFill(strategyName, fill);
						}
						break;
					case SELL:
						if(order.getPrice() <= price - 1) {
							Fill fill = toFill(order, orderId, symbol);
							lstRemoveOrderId.add(orderId);
							String strategyName = mapOrderIdToStrategy.get(orderId);
							addFill(strategyName, fill);
						}
						break;
					}
				}
				// remove
				for(String orderId : lstRemoveOrderId) {
					mapSymbolOrders.remove(orderId);
				}
			}
		}
		
		private Fill toFill(Order order, String orderId, String symbol) {
			String userName = mapOrderIdToUserName.get(orderId);
			Fill fill = new Fill();
			fill.setBuySell(order.getBuySell());
			fill.setFillPrice(String.valueOf(order.getPrice()));
			fill.setOrderId(orderId);
			fill.setOrderPrice(String.valueOf(order.getPrice()));
			fill.setQty(String.valueOf(order.getQty()));
			fill.setFee("0");
			fill.setSymbol(symbol);
			fill.setTimestamp(System.currentTimeMillis());
			fill.setUserName(userName);
			return fill;
		}
	}
}
