package name.qd.tp2.strategies;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.qd.tp2.constants.BuySell;
import name.qd.tp2.constants.Constants;
import name.qd.tp2.constants.TrailingStatus;
import name.qd.tp2.exchanges.ExchangeManager;
import name.qd.tp2.exchanges.vo.Fill;
import name.qd.tp2.exchanges.vo.MarketInfo;
import name.qd.tp2.exchanges.vo.Orderbook;
import name.qd.tp2.exchanges.vo.TrailingOrder;
import name.qd.tp2.strategies.config.StrategyConfig;
import name.qd.tp2.utils.PriceUtils;

public abstract class AbstractStrategy implements Strategy {
	private Logger log = LoggerFactory.getLogger(AbstractStrategy.class); 
	protected ExchangeManager exchangeManager = ExchangeManager.getInstance();
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	
	protected String strategyName;
	protected StrategyConfig strategyConfig;
	private boolean isStrategyReady = false;
	
	private Map<String, TrailingOrder> mapTrailingOrder = new HashMap<>();
	// order id from exchange, 自編order id
	private Map<String, String> mapTrailingOrderId = new HashMap<>();
	
	private List<Fill> lstFill = new ArrayList<>();
	
	public AbstractStrategy(String strategyName, StrategyConfig strategyConfig) {
		this.strategyName = strategyName;
		this.strategyConfig = strategyConfig;
		
		initAllExchange();
		
		// TODO 搬到最後
		while(true) {
			// TODO 這邊要改成實作一個 isExchangeWebsocketReady
			if(isAllExchangeReady()) {
				break;
			}
			
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		// TODO 這兩個subscribe完 exchange才算真的ready
		subscribeSymbols();
		setUserInfo();
		
		isStrategyReady = true;
	}
	
	public void start() {
		while(true) {
			if(isStrategyReady) {
				executor.execute(new StrategyCycle());
				break;
			}
		}
	}
	
	public abstract void strategyAction();
	
	protected String sendTrailingOrder(String exchange, String userName, String symbol, BuySell buySell, double price, double qty, BigDecimal tickSize) {
		TrailingOrder trailingOrder = new TrailingOrder();
		trailingOrder.setStrategyName(strategyName);
		trailingOrder.setExchange(exchange);
		trailingOrder.setUserName(userName);
		trailingOrder.setSymbol(symbol);
		trailingOrder.setBuySell(buySell);
		BigDecimal bigDecimal = PriceUtils.trimPriceWithTicksize(BigDecimal.valueOf(price), tickSize, RoundingMode.UP);
		trailingOrder.setPrice(bigDecimal.doubleValue());
		trailingOrder.setQty(qty);
		
		String uuid = UUID.randomUUID().toString();
		mapTrailingOrder.put(uuid, trailingOrder);
		
		log.info("Put trailing order {} {}", price, uuid);
		return uuid;
	}
	
	protected boolean cancelOrder(String exchange, String userName, String symbol, String orderId) {
		String exId = null;
		boolean cancelSuccess = false;
		if(mapTrailingOrder.containsKey(orderId)) {
			// trailing order
			for(String exOrderId : mapTrailingOrderId.keySet()) {
				if(mapTrailingOrderId.get(exOrderId).equals(orderId)) {
					exId = exOrderId;
					break;
				}
			}
			
			log.info("cancel trailing order {}", orderId);
			mapTrailingOrder.remove(orderId);
			cancelSuccess = true;
			// triggered的才會有exchange id
			if(exId != null) {
				log.info("cancel trailing order, already send to exchange {} {}", orderId, exId);
				cancelSuccess = exchangeManager.cancelOrder(strategyName, exchange, userName, symbol, exId);
			}
		} else {
			// normal order
			return exchangeManager.cancelOrder(strategyName, exchange, userName, symbol, orderId);
		}
		
		if(cancelSuccess) {
			mapTrailingOrder.remove(orderId);
			mapTrailingOrderId.remove(exId);
		}
		
		return cancelSuccess;
	}
	
	protected String sendLimitOrder(String exchange, String userName, String symbol, BuySell buySell, double price, double qty, BigDecimal tickSize) {
		BigDecimal bigDecimal = PriceUtils.trimPriceWithTicksize(BigDecimal.valueOf(price), tickSize, RoundingMode.UP);
		return exchangeManager.sendLimitOrder(strategyName, exchange, userName, symbol, buySell, bigDecimal.doubleValue(), qty);
	}
	
	protected String sendMarketOrder(String exchange, String userName, String symbol, BuySell buySell, double qty) {
		return exchangeManager.sendMarketOrder(strategyName, exchange, userName, symbol, buySell, qty);
	}
	
	private void initAllExchange() {
		Set<String> exchanges = strategyConfig.getAllExchange();
		for(String exchange : exchanges) {
			String env = strategyConfig.getExchangeEvn(exchange);
			String fillChannel = strategyConfig.getExchangeFillChannel(exchange);
			exchangeManager.initExchange(exchange, env, fillChannel);
		}
	}
	
	private void setUserInfo() {
		Set<String> exchanges = strategyConfig.getAllExchange();
		for(String exchange : exchanges) {
			Set<String> users = strategyConfig.getAllUser(exchange);
			for(String userName : users) {
				exchangeManager.addUserApiKeySecret(exchange, userName, strategyConfig.getApiKeySecret(exchange, userName));
			}
		}
	}
	
	private boolean isAllExchangeReady() {
		Set<String> exchanges = strategyConfig.getAllExchange();
		int readyCount = 0;
		for(String exchange : exchanges) {
			if(exchangeManager.isExchangeReady(exchange)) {
				readyCount++;
			} else {
				log.error("{} exchange not ready.", exchange);
			}
		}
		return exchanges.size() == readyCount;
	}
	
	private void subscribeSymbols() {
		Set<String> exchanges = strategyConfig.getAllExchange();
		for(String exchange : exchanges) {
			Set<String> symbols = strategyConfig.getAllSymbols(exchange);
			for(String symbol : symbols) {
				if(Constants.ALL_SYMBOLS.equals(symbol)) {
					subscribeAllSymbols(exchange);
				} else {
					exchangeManager.subscribe(exchange, symbol);
				}
			}
		}
	}
	
	private void subscribeAllSymbols(String exchange) {
		List<MarketInfo> lst = exchangeManager.getMarkets(exchange);
		for(MarketInfo marketInfo : lst) {
			exchangeManager.subscribe(exchange, marketInfo.getSymbol());
		}
	}
	
	private void checkTrailingPrice() {
		for(String uuid : mapTrailingOrder.keySet()) {
			TrailingOrder trailingOrder = mapTrailingOrder.get(uuid);
			Orderbook orderbook = exchangeManager.getOrderbook(trailingOrder.getExchange(), trailingOrder.getSymbol());
			BuySell buySell = trailingOrder.getBuySell();
			
			if(trailingOrder.getTrailingStatus() == TrailingStatus.TRAILING_STATUS_NONE) {
				double pullbackTolerance = strategyConfig.getPullbackTolerance();
				// 等待過界
				if(buySell == BuySell.BUY) {
					// 買單低於
					double marketSellPrice = orderbook.getAskTopPrice(1)[0];
					if(marketSellPrice <= trailingOrder.getPrice() - pullbackTolerance) {
						trailingOrder.setTrailingStatus(TrailingStatus.TRAILING_STATUS_TRIIGERED);
						trailingOrder.setEdgePrice(marketSellPrice);
						
						log.info("Trailing order triggered. {} {}", trailingOrder.getPrice(), uuid);
					}
				} else {
					double marketBuyPrice = orderbook.getBidTopPrice(1)[0];
					if(marketBuyPrice >= trailingOrder.getPrice() + pullbackTolerance) {
						trailingOrder.setTrailingStatus(TrailingStatus.TRAILING_STATUS_TRIIGERED);
						trailingOrder.setEdgePrice(marketBuyPrice);
						
						log.info("Trailing order triggered. {} {}", trailingOrder.getPrice(), uuid);
					}
				}
			} else if(trailingOrder.getTrailingStatus() == TrailingStatus.TRAILING_STATUS_TRIIGERED) {
				// 算最新邊境價格 或是已經 pull back
				double edgePrice = trailingOrder.getEdgePrice();
				if(buySell == BuySell.BUY) {
					double marketSellPrice = orderbook.getAskTopPrice(1)[0];
					
					if(marketSellPrice < edgePrice) {
						trailingOrder.setEdgePrice(marketSellPrice);
					} else {
						// 市場賣單已經高於邊境價格 回彈中
						
						// 市場賣單已經高於下單價格
						if(marketSellPrice > trailingOrder.getPrice()) {
							sendTrailingLimitOrder(uuid, trailingOrder);
							
							log.info("pull back and send {} {}", trailingOrder.getPrice(), uuid);
						} else if(marketSellPrice >= edgePrice + strategyConfig.getTrailingValue()) {
							trailingOrder.setPrice(marketSellPrice);
							sendTrailingLimitOrder(uuid, trailingOrder);
							
							log.info("pull back and send {} {}", trailingOrder.getPrice(), uuid);
						}
					}
				} else {
					double marketBuyPrice = orderbook.getBidTopPrice(1)[0];
					
					if(marketBuyPrice > edgePrice) {
						trailingOrder.setEdgePrice(marketBuyPrice);
					} else {
						if(marketBuyPrice < trailingOrder.getPrice()) {
							sendTrailingLimitOrder(uuid, trailingOrder);
							
							log.info("pull back and send {} {}", trailingOrder.getPrice(), uuid);
						} else if(marketBuyPrice <= edgePrice - strategyConfig.getTrailingValue()) {
							trailingOrder.setPrice(marketBuyPrice);
							sendTrailingLimitOrder(uuid, trailingOrder);
							
							log.info("pull back and send {} {}", trailingOrder.getPrice(), uuid);
						}
					}
				}
			}
		}
	}
	
	private void sendTrailingLimitOrder(String uuid, TrailingOrder trailingOrder) {
		String orderId = exchangeManager.sendLimitOrder(strategyName, trailingOrder.getExchange(), trailingOrder.getUserName(), trailingOrder.getSymbol(), trailingOrder.getBuySell(), trailingOrder.getPrice(), trailingOrder.getQty());
		if(orderId == null) {
			log.error("Send order failed.");
			return;
		}
		
		trailingOrder.setTrailingStatus(TrailingStatus.TRAILING_STATUS_SENDED);
		mapTrailingOrderId.put(orderId, uuid);
	}
	
	private void checkFill() {
		for(String exchange : strategyConfig.getAllExchange()) {
			List<Fill> lst = exchangeManager.getFill(strategyName, exchange);
			
			for(Fill fill : lst) {
				if(mapTrailingOrderId.containsKey(fill.getOrderId())) {
					String trailingOrderId = mapTrailingOrderId.get(fill.getOrderId());
					fill.setOrderId(trailingOrderId);
					
					log.info("trailing order fill {} {}", fill.getFillPrice(), trailingOrderId);
				}
			}
			
			lstFill.addAll(lst);
		}
	}
	
	protected List<Fill> getFill() {
		List<Fill> lst = new ArrayList<>(lstFill);
		lstFill.clear();
		return lst;
	}
	
	private class StrategyCycle implements Runnable {
		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				// check trailing price
				checkTrailingPrice();
				
				// check trailing fill
				// 把fill先拿出來檢查trailing
				// 因為trailing是自編orderId
				checkFill();
				
				strategyAction();
				
				// 睡一下電腦要爆了
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
