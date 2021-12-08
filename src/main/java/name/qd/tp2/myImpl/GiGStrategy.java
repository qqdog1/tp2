package name.qd.tp2.myImpl;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.qd.fileCache.FileCacheManager;
import name.qd.fileCache.cache.NormalCacheManager;
import name.qd.tp2.constants.BuySell;
import name.qd.tp2.constants.StopProfitType;
import name.qd.tp2.exchanges.ExchangeManager;
import name.qd.tp2.exchanges.vo.Fill;
import name.qd.tp2.exchanges.vo.Orderbook;
import name.qd.tp2.myImpl.vo.Grid1StrategyStatus;
import name.qd.tp2.strategies.AbstractStrategy;
import name.qd.tp2.strategies.config.JsonStrategyConfig;
import name.qd.tp2.strategies.config.StrategyConfig;
import name.qd.tp2.utils.LineNotifyUtils;
import name.qd.tp2.utils.PriceUtils;

/**
 * Grid in Grid
 * 在等比網格中 再加入等差網格
 * Grid1成交後 開始Grid2
 * Grid2成交 降低Grid1成本
 * 加速Grid1獲利速度
 * 
 * 等比網格成交後 計算均價及停利價
 * 等差網格第一單在均價(或停利價)往下N(1或1.5之類)個等差
 * 等差網格只要單向成交 就重新計算預期均價及預期停利
 * 因為要停到利表示等差反向單必先成交
 */
public class GiGStrategy extends AbstractStrategy {
	private Logger log = LoggerFactory.getLogger(GiGStrategy.class);
	private LineNotifyUtils lineNotifyUtils;
	// 與交易所溝通用
	private ExchangeManager exchangeManager = ExchangeManager.getInstance();

	private static String strategyName = "[GiG]";
	private String userName = "shawn";

	// 自己設定一些策略內要用的變數
	private String symbol = "ETHPFC";
	private String stopProfitOrderId;
	private BigDecimal targetPrice;
	private BigDecimal firstOrderMarketPrice;
	private Set<String> setOrderId = new HashSet<>();

	// 這個策略要用到的值 全部都設定在config
	private BigDecimal g1PriceRange;
	private int g1FirstContractSize;
	private StopProfitType g1StopProfitType;
	private BigDecimal g1StopProfit;
	private int g1OrderLevel;
	private BigDecimal g1CeilingPrice;
	
	private BigDecimal g2PriceRange;
	private int g2OrderSize;
	private StopProfitType g2StopProfitType;
	private BigDecimal g2StopProfit;
	private int g2OrderLevel;
	private int g2MaxSize;
	
	private BigDecimal tickSize;
	private String lineNotify;

	private long from;
	private long to;

	private FileCacheManager fileCacheManager;
	private NormalCacheManager g1CacheManager;
	private Grid1StrategyStatus g1StrategyStatus;
	
	private String tradingExchange;

	public GiGStrategy(StrategyConfig strategyConfig) {
		super(strategyName, strategyConfig);

		// config settings for g1
		g1PriceRange = new BigDecimal(strategyConfig.getCustomizeSettings("g1_priceRange"));
		g1FirstContractSize = Integer.parseInt(strategyConfig.getCustomizeSettings("g1_firstContractSize"));
		g1StopProfitType = StopProfitType.valueOf(strategyConfig.getCustomizeSettings("g1_stopProfitType"));
		g1StopProfit = new BigDecimal(strategyConfig.getCustomizeSettings("g1_stopProfit"));
		g1OrderLevel = Integer.parseInt(strategyConfig.getCustomizeSettings("g1_orderLevel"));
		g1CeilingPrice = new BigDecimal(strategyConfig.getCustomizeSettings("g1_ceilingPrice"));
		
		// g2 settings
		g2PriceRange = new BigDecimal(strategyConfig.getCustomizeSettings("g2_priceRange"));
		g2OrderSize = Integer.parseInt(strategyConfig.getCustomizeSettings("g2_orderSize"));
		g2StopProfitType = StopProfitType.valueOf(strategyConfig.getCustomizeSettings("g2_stopProfitType"));
		g2StopProfit = new BigDecimal(strategyConfig.getCustomizeSettings("g2_stopProfit"));
		g2OrderLevel = Integer.parseInt(strategyConfig.getCustomizeSettings("g2_orderLevel"));
		g2MaxSize = Integer.parseInt(strategyConfig.getCustomizeSettings("g2_maxSize"));
		
		tickSize = new BigDecimal(strategyConfig.getCustomizeSettings("tickSize"));
		lineNotify = strategyConfig.getCustomizeSettings("lineNotify");
		tradingExchange = strategyConfig.getCustomizeSettings("tradingExchange");
		
		lineNotifyUtils = new LineNotifyUtils(lineNotify);

		fileCacheManager = new FileCacheManager("./gig");

		initAndRestoreCache();
	}

	private void initAndRestoreCache() {
		try {
			g1CacheManager = fileCacheManager.getNormalCacheInstance("g1", Grid1StrategyStatus.class.getName());
		} catch (Exception e) {
			log.error("init cache failed", e);
			lineNotifyUtils.sendMessage(strategyName, "init cache failed");
		}

		g1StrategyStatus = (Grid1StrategyStatus) g1CacheManager.get("g1");
		if (g1StrategyStatus == null) {
			g1StrategyStatus = new Grid1StrategyStatus();
			g1CacheManager.put(g1StrategyStatus);
		} else {
			log.info("重新啟動策略 前次第一單:{}, 均價:{}, 未平量:{}", g1StrategyStatus.getFirstOrderPrice(), g1StrategyStatus.getAveragePrice(), g1StrategyStatus.getPosition());
			targetPrice = PriceUtils.getStopProfitPrice(BigDecimal.valueOf(g1StrategyStatus.getAveragePrice()), g1StopProfitType, g1StopProfit);
			placeStopProfitOrder();
			
			int remainPosition = g1StrategyStatus.getPosition();
			double basePrice = g1StrategyStatus.getFirstOrderPrice();
			for (int i = 0; i < g1OrderLevel; i++) {
				basePrice = basePrice - (g1PriceRange.intValue() * Math.pow(2, i));
				double qty = g1FirstContractSize * Math.pow(2, i);
				
				if(remainPosition >= qty) {
					remainPosition -= qty;
				} else {
					qty -= remainPosition;
					remainPosition = 0;
					
					String orderId = sendOrder(BuySell.BUY, basePrice, qty);
					if (orderId != null) {
						log.info("鋪單 {} {} {} {}", i, basePrice, qty, orderId);
						setOrderId.add(orderId);
					} else {
						log.error("鋪單失敗 {} {} {}", i, basePrice, qty);
						lineNotifyUtils.sendMessage(strategyName + "鋪單失敗");
					}
				}
			}
		}
	}

	@Override
	public void strategyAction() {
		// from websocket, Fake exchange use this
		checkFill();
		
		// for some exchange had unstable websocket connection
//		checkFillRest();

		// 策略剛啟動鋪單
		if (setOrderId.size() == 0) {
			Orderbook orderbook = exchangeManager.getOrderbook(tradingExchange, symbol);
			if (orderbook == null) return;
			double price = orderbook.getBidTopPrice(1)[0];
			// 紀錄下第一單時 當下的市場價格
			firstOrderMarketPrice = BigDecimal.valueOf(price);
			g1StrategyStatus.setFirstOrderPrice(price);
			if(firstOrderMarketPrice.compareTo(g1CeilingPrice) > 0) {
				// 價格已超過天花板
				return;
			}
			placeLevelOrders(0, price);
			
			try {
				g1CacheManager.writeCacheToFile();
			} catch (IOException e) {
				log.error("write cache to file failed.", e);
			}
		} else {
			checkCurrentPrice();
		}
	}

	private void checkCurrentPrice() {
		// 已鋪單 但未成交狀況下
		// 價格上漲難已成交
		// 刪除全部未成交單
		if (g1StrategyStatus.getPosition() == 0) {
			// 第一單完全成交狀態下
			Orderbook orderbook = exchangeManager.getOrderbook(tradingExchange, symbol);
			if (orderbook != null) {
				double price = orderbook.getBidTopPrice(1)[0];
				BigDecimal stopProfitPrice = PriceUtils.getStopProfitPrice(firstOrderMarketPrice.subtract(g1PriceRange), g1StopProfitType, g1StopProfit);
				if (price >= stopProfitPrice.doubleValue()) {
					log.info("價格上漲 重新鋪單");
					// 刪除所有鋪單
					cancelOrder(null);
				}
			}
		}
	}

	private void checkFill() {
		// TODO 測試Fake Exchange要改回這個
		// 或是有穩定的websocket的交易所可用這個
		List<Fill> lstFill = exchangeManager.getFill(strategyName, tradingExchange);
		
		processFill(lstFill);
	}
	
//	private void checkFillRest() {
//		ZonedDateTime zonedDateTime = ZonedDateTime.now();
//		to = zonedDateTime.toEpochSecond() * 1000;
//		List<Fill> lstFill = exchangeManager.getFillHistory(tradingExchange, userName, symbol, from, to);
//		if (lstFill == null) return;
//		from = to;
//		
//		processFill(lstFill);
//	}
	
	private void processFill(List<Fill> lst) {
		for (Fill fill : lst) {
			// 濾掉不是此策略的成交
			if (!setOrderId.contains(fill.getOrderId()) && !fill.getOrderId().equals(stopProfitOrderId)) continue;

			log.debug("收到成交 {} {} {}", fill.getFillPrice(), fill.getQty(), fill.getOrderId());
			int qty = (int) Double.parseDouble(fill.getQty());
			double fillPrice = Double.parseDouble(fill.getFillPrice());
			if (fill.getOrderId().equals(stopProfitOrderId)) {
				g1StrategyStatus.setPosition(g1StrategyStatus.getPosition() - qty);
				// 停利單成交
				if (g1StrategyStatus.getPosition() == 0) {
					log.info("停利單完全成交, {} {} {}", fill.getFillPrice(), fill.getQty(), fill.getOrderId());
					lineNotifyUtils.sendMessage(strategyName + "停利單完全成交");
					stopProfitOrderId = null;
					// 計算獲利
					calcProfit(qty, fillPrice, Double.parseDouble(fill.getFee()));
					// 清除成本
					g1StrategyStatus.setAveragePrice(0);
					// 重算目標價
					targetPrice = PriceUtils.getStopProfitPrice(BigDecimal.valueOf(g1StrategyStatus.getAveragePrice()), g1StopProfitType, g1StopProfit);
					// 刪除之前鋪單
					cancelOrder(null);
				} else {
					log.warn("停利單部分成交 {}, {} {}", fill.getFillPrice(), fill.getQty(), fill.getOrderId());
					lineNotifyUtils.sendMessage(strategyName + "停利單部分成交" + fill.getQty());
					calcProfit(qty, fillPrice, Double.parseDouble(fill.getFee()));
				}
			} else {
				// 一般單成交
				
				// 重算成本 改停利單
				double averagePrice = ((g1StrategyStatus.getPosition() * g1StrategyStatus.getAveragePrice()) + (fillPrice * qty)) / (g1StrategyStatus.getPosition() + qty);
				g1StrategyStatus.setAveragePrice(averagePrice);
				g1StrategyStatus.setPosition(g1StrategyStatus.getPosition() + qty);
				// 清除舊的停利單
				if (stopProfitOrderId != null) {
					if (!cancelOrder(stopProfitOrderId)) {
						log.error("清除舊的停利單失敗 orderId:{}", stopProfitOrderId);
						lineNotifyUtils.sendMessage(strategyName + "清除舊的停利單失敗");
					} else {
						stopProfitOrderId = null;
						log.info("清除舊的停利單");
					}
				}
				// 下新的停利單
				targetPrice = PriceUtils.getStopProfitPrice(BigDecimal.valueOf(averagePrice), g1StopProfitType, g1StopProfit);
				placeStopProfitOrder();
			}
			
			try {
				g1CacheManager.writeCacheToFile();
			} catch (IOException e) {
				log.error("write cache to file failed.", e);
			}
		}
	}

	private void placeLevelOrders(int startLevel, double basePrice) {
		for (int i = 0; i < g1OrderLevel; i++) {
			basePrice = basePrice - (g1PriceRange.intValue() * Math.pow(2, i));
			double qty = g1FirstContractSize * Math.pow(2, i);
			if (i == startLevel) {
				String orderId = sendOrder(BuySell.BUY, basePrice, qty);
				if (orderId != null) {
					log.info("鋪單 {} {} {} {}", i, basePrice, qty, orderId);
					setOrderId.add(orderId);
				} else {
					log.error("鋪單失敗 {} {} {}", i, basePrice, qty);
					lineNotifyUtils.sendMessage(strategyName + "鋪單失敗");
				}
				startLevel++;
			}
		}
	}

	private void placeStopProfitOrder() {
		String orderId = sendOrder(BuySell.SELL, targetPrice.doubleValue(), g1StrategyStatus.getPosition());
		if (orderId != null) {
			stopProfitOrderId = orderId;
			log.info("下停利單 {} {} {}", targetPrice, g1StrategyStatus.getPosition(), orderId);
		} else {
			log.warn("下停利單失敗 {} {}", targetPrice, g1StrategyStatus.getPosition());
			lineNotifyUtils.sendMessage(strategyName + "下停利單失敗");
		}
	}

	private String sendOrder(BuySell buySell, double price, double qty) {
		BigDecimal bigDecimal = PriceUtils.trimPriceWithTicksize(BigDecimal.valueOf(price), tickSize, RoundingMode.UP);
		return exchangeManager.sendOrder(strategyName, tradingExchange, userName, symbol, buySell, bigDecimal.doubleValue(), qty);
	}

	private boolean cancelOrder(String orderId) {
		if (orderId == null) {
			for (String id : setOrderId) {
				if (id != null) {
					exchangeManager.cancelOrder(strategyName, tradingExchange, userName, symbol, id);
				}
			}
			setOrderId.clear();
			return true;
		} else {
			return exchangeManager.cancelOrder(strategyName, tradingExchange, userName, symbol, orderId);
		}
	}

	private void calcProfit(double qty, double price, double fee) {
		double priceDiff = price - g1StrategyStatus.getAveragePrice();
		double profit = (priceDiff * qty / 100) - fee;
		lineNotifyUtils.sendMessage(strategyName + "獲利: " + profit);
		log.info("獲利: {}", profit);
	}

	public static void main(String[] s) {
		Properties prop = System.getProperties();
		prop.setProperty("log4j.configurationFile", "./config/log4j2.xml");

		try {
			String configPath = "./config/gig.json";
			GiGStrategy strategy = new GiGStrategy(new JsonStrategyConfig(configPath));
			strategy.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
