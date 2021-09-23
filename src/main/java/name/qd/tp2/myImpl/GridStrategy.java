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
import name.qd.tp2.strategies.AbstractStrategy;
import name.qd.tp2.strategies.config.JsonStrategyConfig;
import name.qd.tp2.strategies.config.StrategyConfig;
import name.qd.tp2.utils.LineNotifyUtils;
import name.qd.tp2.utils.PriceUtils;

/**
 * 進場: 等比價量下跌吸貨 
 * 		EX: 
 * 		設定第一單contract size = 10, price range = 10 
 * 		若現在價格為3010
 * 		第一單將於3000(3010 - price range * 1)吸貨10(contract size * 1)張 
 * 		第二單將於2980(3000 - price range * 2)吸貨 20(contract size * 2)張 
 * 		第三單將於2940(2990 - price range * 4)吸貨 40(contract size * 4)張 
 * 		... 
 * 		若第一單遲遲未成交 且價格上漲超過一定價格 
 * 		會刪除全部未成交單 
 * 		重新以新現價計算後鋪單 
 * 出場:
 * 		均價以上定值或定比例出貨 
 * 		EX: 
 * 		依照進場均價 
 * 		若設定定值出場 出場價 = 均價 + profit price 
 * 		若設定獲利趴數出場 出場價 = 均價 * (1 + profit rate) 
 * 其他: 
 * 		系統沒有cache orders(看策略類型規劃 目前不需要 有的話系統要計算partial fill清cache) 
 * 		所以reboot後如果有成交會無法派送回給當初下單的策略 
 * 		因此系統關機後必須清空所有未成交orders 
 * 		strategy level cache紀錄關機前狀態 未平倉量 均價 以成交細節等等
 */
public class GridStrategy extends AbstractStrategy {
	private Logger log = LoggerFactory.getLogger(GridStrategy.class);
	private LineNotifyUtils lineNotifyUtils;
	// 與交易所溝通用
	private ExchangeManager exchangeManager = ExchangeManager.getInstance();

	private String strategyName = "[等比網格]";
	private String userName = "shawn";

	// 自己設定一些策略內要用的變數
	private String symbol = "ETHPFC";
	private String stopProfitOrderId;
	private BigDecimal targetPrice;
	private BigDecimal firstOrderMarketPrice;
	private Set<String> setOrderId = new HashSet<>();

	// 這個策略要用到的值 全部都設定在config
	private BigDecimal priceRange;
	private int firstContractSize;
	private StopProfitType stopProfitType;
	private BigDecimal stopProfit;
	private BigDecimal tickSize;
	private int orderLevel;
	private String lineNotify;

	private long from;
	private long to;

	private FileCacheManager fileCacheManager;
	private NormalCacheManager grid1CacheManager;
	private Grid1StrategyStatus grid1StrategyStatus;

	public GridStrategy(StrategyConfig strategyConfig) {
		super(strategyConfig);

		// 把設定值從config讀出來
		priceRange = new BigDecimal(strategyConfig.getCustomizeSettings("priceRange"));
		firstContractSize = Integer.parseInt(strategyConfig.getCustomizeSettings("firstContractSize"));
		stopProfitType = StopProfitType.valueOf(strategyConfig.getCustomizeSettings("stopProfitType"));
		stopProfit = new BigDecimal(strategyConfig.getCustomizeSettings("stopProfit"));
		tickSize = new BigDecimal(strategyConfig.getCustomizeSettings("tickSize"));
		orderLevel = Integer.parseInt(strategyConfig.getCustomizeSettings("orderLevel"));
		lineNotify = strategyConfig.getCustomizeSettings("lineNotify");

		lineNotifyUtils = new LineNotifyUtils(lineNotify);

		ZonedDateTime zonedDateTime = ZonedDateTime.now();
		from = zonedDateTime.toEpochSecond() * 1000;
		to = zonedDateTime.toEpochSecond() * 1000;

		fileCacheManager = new FileCacheManager("./grid1");

		initAndRestoreCache();
	}

	private void initAndRestoreCache() {
		try {
			grid1CacheManager = fileCacheManager.getNormalCacheInstance("grid1", Grid1StrategyStatus.class.getName());
		} catch (Exception e) {
			log.error("init cache failed", e);
			lineNotifyUtils.sendMessage(strategyName, "init cache failed");
		}

		grid1StrategyStatus = (Grid1StrategyStatus) grid1CacheManager.get("grid1");
		if (grid1StrategyStatus == null) {
			grid1StrategyStatus = new Grid1StrategyStatus();
			grid1CacheManager.put(grid1StrategyStatus);
		}
	}

	@Override
	public void strategyAction() {
		checkFill();

		// 策略剛啟動鋪單
		if (setOrderId.size() == 0) {
			Orderbook orderbook = exchangeManager.getOrderbook(ExchangeManager.BTSE_EXCHANGE_NAME, symbol);
			if (orderbook == null) return;
			double price = orderbook.getBidTopPrice(1)[0];
			// 紀錄下第一單時 當下的市場價格
			firstOrderMarketPrice = BigDecimal.valueOf(price);
			placeLevelOrders(0, price);
		} else {
			checkCurrentPrice();
		}

		// 給GTC多一點時間 且電腦要爆了
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void checkCurrentPrice() {
		// 已鋪單 但未成交狀況下
		// 價格上漲難已成交
		// 刪除全部未成交單
		if (grid1StrategyStatus.getPosition() == 0) {
			// 第一單完全成交狀態下
			Orderbook orderbook = exchangeManager.getOrderbook(ExchangeManager.BTSE_EXCHANGE_NAME, symbol);
			if (orderbook != null) {
				double price = orderbook.getBidTopPrice(1)[0];
				BigDecimal stopProfitPrice = PriceUtils.getStopProfitPrice(firstOrderMarketPrice.subtract(priceRange),
						stopProfitType, stopProfit);
				if (price >= stopProfitPrice.doubleValue()) {
					log.info("價格上漲 重新鋪單");
					// 刪除所有鋪單
					cancelOrder(null);
				}
			}
		}
	}

	private void checkFill() {
		ZonedDateTime zonedDateTime = ZonedDateTime.now();
		to = zonedDateTime.toEpochSecond() * 1000;

		List<Fill> lstFill = exchangeManager.getFillHistory(ExchangeManager.BTSE_EXCHANGE_NAME, userName, symbol, from, to);

		if (lstFill == null)
			return;
		from = to;

//		List<Fill> lstFill = exchangeManager.getFill(strategyName, ExchangeManager.BTSE_EXCHANGE_NAME);
		for (Fill fill : lstFill) {
			// 濾掉不是此策略的成交
			if (!setOrderId.contains(fill.getOrderId()) && !fill.getOrderId().equals(stopProfitOrderId))
				continue;

			log.debug("收到成交 {} {} {}", fill.getOrderPrice(), fill.getQty(), fill.getOrderId());
			int qty = Integer.parseInt(fill.getQty());
			double orderPrice = Double.parseDouble(fill.getOrderPrice());
			if (fill.getOrderId().equals(stopProfitOrderId)) {
				// 停利單成交
				grid1StrategyStatus.setPosition(grid1StrategyStatus.getPosition() - qty);
				if (grid1StrategyStatus.getPosition() == firstContractSize) {
					log.info("停利單完全成交");
					lineNotifyUtils.sendMessage(strategyName + "停利單完全成交");
					stopProfitOrderId = null;
					// 計算獲利
					calcProfit(qty, orderPrice);
					// 重算成本
					grid1StrategyStatus.setAveragePrice(orderPrice);
					// 重算目標價
					targetPrice = PriceUtils.getStopProfitPrice(BigDecimal.valueOf(grid1StrategyStatus.getAveragePrice()), stopProfitType, stopProfit);
					// 刪除之前鋪單
					cancelOrder(null);
				} else {
					log.warn("停利單部分成交 {}, {}", fill.getOrderPrice(), fill.getQty());
					lineNotifyUtils.sendMessage(strategyName + "停利單部分成交" + fill.getQty());
					calcProfit(qty, orderPrice);
				}
			} else {
				// 一般單成交

				// 重算成本 改停利單
				double averagePrice = ((grid1StrategyStatus.getPosition() * grid1StrategyStatus.getAveragePrice()) + (orderPrice * qty)) / (grid1StrategyStatus.getPosition() + qty);
				grid1StrategyStatus.setAveragePrice(averagePrice);
				grid1StrategyStatus.setPosition(grid1StrategyStatus.getPosition() + qty);
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
				targetPrice = PriceUtils.getStopProfitPrice(BigDecimal.valueOf(averagePrice), stopProfitType, stopProfit);
				placeStopProfitOrder();
			}
			
			try {
				grid1CacheManager.writeCacheToFile();
			} catch (IOException e) {
				log.error("write cache to file failed.", e);
			}
		}
	}

	private void placeLevelOrders(int startLevel, double basePrice) {
		for (int i = 0; i < orderLevel; i++) {
			basePrice = basePrice - (priceRange.intValue() * Math.pow(2, i));
			double qty = firstContractSize * Math.pow(2, i);
			if (i == startLevel) {
				String orderId = sendOrder(BuySell.BUY, basePrice, qty);
				if (orderId != null) {
					log.info("鋪單 {} {} {}", i, basePrice, qty);
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
		if (grid1StrategyStatus.getPosition() - firstContractSize > 0) {
			String orderId = sendOrder(BuySell.SELL, targetPrice.doubleValue(), grid1StrategyStatus.getPosition() - firstContractSize);
			if (orderId != null) {
				stopProfitOrderId = orderId;
				log.info("下停利單 {} {} {}", targetPrice, grid1StrategyStatus.getPosition() - firstContractSize, orderId);
			} else {
				log.warn("下停利單失敗 {} {}", targetPrice, grid1StrategyStatus.getPosition() - firstContractSize);
				lineNotifyUtils.sendMessage(strategyName + "下停利單失敗");
			}
		}
	}

	private String sendOrder(BuySell buySell, double price, double qty) {
		PriceUtils.trimPriceWithTicksize(BigDecimal.valueOf(price), tickSize, RoundingMode.UP);
		return exchangeManager.sendOrder(strategyName, ExchangeManager.BTSE_EXCHANGE_NAME, userName, symbol, buySell, price, qty);
	}

	private boolean cancelOrder(String orderId) {
		if (orderId == null) {
			for (String id : setOrderId) {
				if (id != null) {
					exchangeManager.cancelOrder(strategyName, ExchangeManager.BTSE_EXCHANGE_NAME, userName, symbol, id);
				}
			}
			setOrderId.clear();
			return true;
		} else {
			return exchangeManager.cancelOrder(strategyName, ExchangeManager.BTSE_EXCHANGE_NAME, userName, symbol, orderId);
		}
	}

	private void calcProfit(double qty, double price) {
		double priceDiff = price - grid1StrategyStatus.getAveragePrice();
		double profit = priceDiff * qty / 100;
		lineNotifyUtils.sendMessage(strategyName + "獲利: " + profit);
		log.info("獲利: {}", profit);
	}

	public static void main(String[] s) {
		Properties prop = System.getProperties();
		prop.setProperty("log4j.configurationFile", "./config/log4j2.xml");

		try {
			String configPath = "./config/testnet.json";
//			String configPath = "./config/test.json";
			GridStrategy strategy = new GridStrategy(new JsonStrategyConfig(configPath));
			strategy.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
