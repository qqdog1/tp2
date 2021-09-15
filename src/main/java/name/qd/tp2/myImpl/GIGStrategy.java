package name.qd.tp2.myImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * 在等比網格中 再加入等差網格
 * Grid1成交後 開始Grid2
 * Grid2成交 降低Grid1成本
 * 加速Grid1獲利速度
 * 
 * 等比網格成交後 計算均價及停利價
 * 等差網格第一單在均價(或停利價)往下N(1或1.5之類)個等差
 * 等差網格只要單向成交 就重新計算預期均價及預期停利
 * 因為要停到利表示等差反向單必先成交
 * 
 * 回復交易 可用FileCache
 * 均價及未平量可算出各單(等比單及等差單)成本??
 */
public class GIGStrategy extends AbstractStrategy {
	private Logger log = LoggerFactory.getLogger(GIGStrategy.class);
	private LineNotifyUtils lineNotifyUtils;
	
	private ExchangeManager exchangeManager = ExchangeManager.getInstance();
	
	private String strategyName = "[GIG]";
	private String userName = "shawn";
	
	private String symbol = "ETHPFC";
	private BigDecimal g1Position = BigDecimal.ZERO;
	private BigDecimal g2Position = BigDecimal.ZERO;
	private BigDecimal position = BigDecimal.ZERO;
	private BigDecimal averagePrice = BigDecimal.ZERO;
	private BigDecimal cost = BigDecimal.ZERO;
	private int notifyMinute = -1;
	
	//
	private Set<String> setG1OrderId = new HashSet<>();
	private String g1StopProfitOrderId;
	
	private Map<String, BigDecimal> mapG2OrderIdToPrice = new HashMap<>();
	private Set<Double> setG2OpenPrice = new HashSet<>();
	private Map<String, BigDecimal> mapG2StopProfitOrderIdToPrice = new HashMap<>();
	
	//
	private BigDecimal g1ChasingPrice;
	private int g1PriceRange;
	private int g1OrderSize;
	private StopProfitType g1StopProfitType;
	private BigDecimal g1StopProfit;
	private int g1OrderLevel;
	private double g1FirstOrderPrice;
	
	private int g2PriceRange;
	private int g2OrderSize;
	private StopProfitType g2StopProfitType;
	private BigDecimal g2StopProfit;
	private int g2OrderLevel;
	private int g2MaxContractSize;
	
	private int buyCount = 0;
	private int sellCount = 0;
	
	private String lineNotify;
	private int reportMinute;
	private BigDecimal tickSize;
	
	private boolean g2Pause = false;
	
	private long from;
	private long to;
	
	public GIGStrategy(StrategyConfig strategyConfig) {
		super(strategyConfig);
		
		// 空手的時候若上漲超過多少要追價
		g1ChasingPrice = new BigDecimal(strategyConfig.getCustomizeSettings("g1_chasingPrice"));
		g1PriceRange = Integer.parseInt(strategyConfig.getCustomizeSettings("g1_priceRange"));
		g1OrderSize = Integer.parseInt(strategyConfig.getCustomizeSettings("g1_orderSize"));
		g1StopProfitType = StopProfitType.valueOf(strategyConfig.getCustomizeSettings("g1_stopProfitType"));
		g1StopProfit = new BigDecimal(strategyConfig.getCustomizeSettings("g1_stopProfit"));
		g1OrderLevel = Integer.parseInt(strategyConfig.getCustomizeSettings("g1_orderLevel"));
		
		g2PriceRange = Integer.parseInt(strategyConfig.getCustomizeSettings("g2_priceRange"));
		g2OrderSize = Integer.parseInt(strategyConfig.getCustomizeSettings("g2_orderSize"));
		g2StopProfitType = StopProfitType.valueOf(strategyConfig.getCustomizeSettings("g2_stopProfitType"));
		g2StopProfit = new BigDecimal(strategyConfig.getCustomizeSettings("g2_stopProfit"));
		g2OrderLevel = Integer.parseInt(strategyConfig.getCustomizeSettings("g2_orderLevel"));
		g2MaxContractSize = Integer.parseInt(strategyConfig.getCustomizeSettings("g2_maxContractSize"));
		
		lineNotify = strategyConfig.getCustomizeSettings("lineNotify");
		tickSize = new BigDecimal(strategyConfig.getCustomizeSettings("tickSize"));
		reportMinute = Integer.parseInt(strategyConfig.getCustomizeSettings("reportMinute"));
		
		lineNotifyUtils = new LineNotifyUtils(lineNotify);
		
		ZonedDateTime zonedDateTime = ZonedDateTime.now();
		from = zonedDateTime.toEpochSecond() * 1000;
		to = zonedDateTime.toEpochSecond() * 1000;
	}

	@Override
	public void strategyAction() {
		// 回復交易判斷
		// 回復交易假設G1之前都是完全成交狀態去計算
		
		// 檢查成交
		checkFill();
		
		// 鋪單
		placeOrder();
		
		// 檢查價格是否漲上去導致第一單難以成交
		checkG1OrderWithCurrentPrice();
		
		// 定時回報
		report();
		
		// 滿倉回報
		checkG2Position();
		
		// 刪掉太遠的單
		cancelFarG2OpenOrder();
		
		// 出場價格大於G1停利單的G2單 全部以G1停利價格掛單
		checkG2Order();
	}
	
	private void checkFill() {
		ZonedDateTime zonedDateTime = ZonedDateTime.now();
		to = zonedDateTime.toEpochSecond() * 1000;
		
		List<Fill> lstFill = exchangeManager.getFillHistory(ExchangeManager.BTSE_EXCHANGE_NAME, userName, symbol, from, to);
		if(lstFill == null) return;
		from = to;
		
		for(Fill fill : lstFill) {
			String orderId = fill.getOrderId();
			// is g1 order
			if(setG1OrderId.contains(orderId)) {
				// g1單成交 
				// 算新的成本
				g1Position = g1Position.add(new BigDecimal(fill.getQty()));
				calcCost(fill);
				// 刪除停利單
				if(g1StopProfitOrderId != null) {
					if(!cancelOrder(g1StopProfitOrderId)) {
						log.error("刪除舊的G1停利單失敗 {}", g1StopProfitOrderId);
						lineNotifyUtils.sendMessage("刪除舊的G1停利單失敗", g1StopProfitOrderId);
						continue;
					} else {
						g1StopProfitOrderId = null;
					}
				}
				// 下新的停利單
				placeG1StopProfitOrder();
			}

			// is g1 stop profit order
			else if(orderId.equals(g1StopProfitOrderId)) {
				// g1停利單成交
				BigDecimal qty = new BigDecimal(fill.getQty());
				g1Position = g1Position.subtract(qty);
				calcCost(fill);
				
				if(g1Position.compareTo(BigDecimal.ZERO) == 0) {
					log.info("G1停利單完全成交");
					g1StopProfitOrderId = null;
					calcG1Profit(fill);
					
					// TODO G1單完全成交後要如何處裡G2剩下的單
				} else {
					log.info("G1停利單部分成交");
					calcG1Profit(fill);
				}
			}
			
			// is g2 order
			else if(mapG2OrderIdToPrice.containsKey(orderId)) {
				/**
				 * 重要
				 * TODO g2下單量若放大要處裡partial fill
				 */
				
				// g2 fill 
				g2Position = g2Position.add(new BigDecimal(fill.getQty()));
				buyCount++;
				// 計算新均價
				calcCost(fill);
				// 下g2停利單
				BigDecimal price = mapG2OrderIdToPrice.remove(fill.getOrderId());
				BigDecimal stopProfitPrice = PriceUtils.getStopProfitPrice(price, g2StopProfitType, g2StopProfit);
				placeG2StopProfitOrder(stopProfitPrice);
			}
			
			// is g2 stop profit order
			else if(mapG2StopProfitOrderIdToPrice.containsKey(orderId)) {
				// g2停利 
				g2Position = g2Position.subtract(new BigDecimal(fill.getQty()));
				sellCount++;
				// 計算新均價
				calcCost(fill);
				// 刪除舊G1停利單
				if(cancelOrder(g1StopProfitOrderId)) {
					// 下新G1停利單
					placeG1StopProfitOrder();
				} else {
					log.error("刪除舊的G1停利單失敗 {}", g1StopProfitOrderId);
					lineNotifyUtils.sendMessage("刪除舊的G1停利單失敗", g1StopProfitOrderId);
				}
			}
			
			// unknown fill
			else {
			}
		}
	}
	
	private void placeOrder() {
		// g1 & g2 都沒單 鋪g1單
		if(setG1OrderId.size() == 0 && mapG2OrderIdToPrice.size() == 0) {
			Orderbook orderbook = exchangeManager.getOrderbook(ExchangeManager.BTSE_EXCHANGE_NAME, symbol);
			if(orderbook == null) return;
			
			placeG1LevelOrder(1, orderbook.getBidTopPrice(1)[0]);
		}
		
		// g1 有單 但0成交 不動
		else if(setG1OrderId.size() > 0 && position.compareTo(BigDecimal.ZERO) == 0) {
		}
		
		// g1 有單且成交 鋪g2單
		else if(g1Position.compareTo(BigDecimal.ZERO) > 0) {
			// g2 單鋪在 average price 下一個 g2 price range
			Orderbook orderbook = exchangeManager.getOrderbook(ExchangeManager.BTSE_EXCHANGE_NAME, symbol);
			if(orderbook == null) return;
			
			BigDecimal currentPrice = BigDecimal.valueOf(orderbook.getBidTopPrice(1)[0]);
			
			if(currentPrice.compareTo(averagePrice) == -1) {
				// 限價在均價之下 可舖單範圍
				placeG2LevelOrder(currentPrice);
			}
		}
	}
	
	private void checkG1OrderWithCurrentPrice() {
		// 還沒成交的狀態下
		if(g1Position.compareTo(BigDecimal.ZERO) == 0) {
			Orderbook orderbook = exchangeManager.getOrderbook(ExchangeManager.BTSE_EXCHANGE_NAME, symbol);
			if(orderbook != null) {
				double price = orderbook.getBidTopPrice(1)[0];
				if(price > g1FirstOrderPrice + g1PriceRange + g1ChasingPrice.doubleValue()) {
					log.info("價格上漲超過追價 currentPrice:{}, 向上墊高G1 orders", price);
					// 刪除G1 orders
					cancelAllG1OpenOrder();
				}
			}
		}
	}
	
	private void report() {
		ZonedDateTime zonedDateTime = ZonedDateTime.now();
		int minute = zonedDateTime.getMinute();
		if(notifyMinute != minute && minute % reportMinute == 0) {
			lineNotifyUtils.sendMessage(strategyName + "現在持倉: " + position + " ,cost: " + cost + " ,平均成本: " + averagePrice + ", 區間買量: " + buyCount + ", 區間賣量: " + sellCount);
			notifyMinute = minute;
			buyCount = 0;
			sellCount = 0;
		}
	}
	
	private void checkG2Position() {
		if(g2Pause) {
			if(g2Position.intValue() < g2MaxContractSize) {
				g2Pause = false;
			}
		} else {
			if(g2Position.intValue() >= g2MaxContractSize) {
				lineNotifyUtils.sendMessage(strategyName + "G2爆倉中, 注意策略狀態");
				cancelAllG2OpenOrder();
				g2Pause = true;
			}
		}
	}
	
	private void cancelFarG2OpenOrder() {
		if(setG2OpenPrice.size() > g2OrderLevel) {
			Collection<BigDecimal> collection = mapG2OrderIdToPrice.values();
			List<BigDecimal> lst = new ArrayList<>(collection);
			Collections.sort(lst);
			
			List<String> lstRemoveOrderId = new ArrayList<>();
			for(int i = 0 ; i < lst.size() - g2OrderLevel; i++) {
				int index = i;
				mapG2OrderIdToPrice.forEach((orderId, price) -> {
					if(price.equals(lst.get(index))) {
						lstRemoveOrderId.add(orderId);
					}
				});
			}
			
			for(String orderId : lstRemoveOrderId) {
				if(cancelOrder(orderId)) {
					BigDecimal price = mapG2OrderIdToPrice.remove(orderId);
					setG2OpenPrice.remove(price.doubleValue());
				}
			}
		}
	}
	
	// 出場價格大於G1停利單的G2單 全部以G1停利價格掛單
	private void checkG2Order() {
		
	}
	
	private void placeG1LevelOrder(int startLevel, double basePrice) {
		for(int i = 1; i <= g1OrderLevel ; i++) {
			basePrice = basePrice - (g1PriceRange * Math.pow(2, i-1));
			double qty = g1OrderSize * Math.pow(2, i-1);
			if(i == startLevel) {
				String orderId = sendOrder(BuySell.BUY, basePrice, qty);
				if(orderId != null) {
					if(i == 1) {
						g1FirstOrderPrice = basePrice;
					}
					log.info("G1 鋪單 {} {} {} {}", i, basePrice, qty, orderId);
					setG1OrderId.add(orderId);
				} else {
					log.error("G1 鋪單失敗 {} {} {}", i, basePrice, qty);
					lineNotifyUtils.sendMessage(strategyName, "G1 鋪單失敗", String.valueOf(basePrice), String.valueOf(qty));
				}
				startLevel++;
			}
		}
	}
	
	private void placeG2LevelOrder(BigDecimal basePrice) {
		basePrice = basePrice.subtract(basePrice.remainder(BigDecimal.valueOf(g2PriceRange)));
		for(int i = 0 ; i < g2OrderLevel ;) {
			BigDecimal price = basePrice.subtract(BigDecimal.valueOf(g2PriceRange).multiply(BigDecimal.valueOf(i)));
			if(!setG2OpenPrice.contains(price.doubleValue())) {
				String orderId = sendOrder(BuySell.BUY, price.doubleValue(), g2OrderSize);
				if(orderId != null) {
					mapG2OrderIdToPrice.put(orderId, price);
					setG2OpenPrice.add(price.doubleValue());
					i++;
				} 
			} else {
				i++;
			}
		}
	}
	
	private void placeG1StopProfitOrder() {
		BigDecimal stopProfitPrice = PriceUtils.getStopProfitPrice(averagePrice, g1StopProfitType, g1StopProfit);
		String orderId = sendOrder(BuySell.SELL, stopProfitPrice.doubleValue(), g1Position.doubleValue());
		if(orderId != null) {
			g1StopProfitOrderId = orderId;
			log.info("G1下停利單 {} {} {}", stopProfitPrice, g1Position, orderId);
		} else {
			log.error("下G1停利單失敗");
			lineNotifyUtils.sendMessage("下G1停利單失敗");
		}
	}
	
	private void placeG2StopProfitOrder(BigDecimal price) {
		String orderId = sendOrder(BuySell.SELL, price.doubleValue(), g2OrderSize);
		if(orderId != null) {
			mapG2StopProfitOrderIdToPrice.put(orderId, price);
			log.info("G2下停利單 {} {} {}", price, g2OrderSize, orderId);
		} else {
			log.error("下G2停利單失敗");
			lineNotifyUtils.sendMessage("下G2停利單失敗", price.toPlainString());
		}
	}
	
	private void calcCost(Fill fill) {
		BigDecimal price = new BigDecimal(fill.getFillPrice());
		BuySell buySell = fill.getBuySell();
		BigDecimal qty = new BigDecimal(fill.getQty());
		if(buySell == BuySell.SELL) {
			qty = qty.negate();
		}
		BigDecimal fee = new BigDecimal(fill.getFee());
		
		cost = cost.add(price.multiply(qty)).add(fee);
		position = position.add(qty);
		if(position.compareTo(BigDecimal.ZERO) != 0) {
			averagePrice = cost.divide(position, 8, RoundingMode.DOWN);
		} else {
			averagePrice = BigDecimal.ZERO;
		}
		
		log.info("當前成本 {}, {} {}", cost, position, averagePrice);
	}
	
	private void calcG1Profit(Fill fill) {
		BigDecimal priceDiff = new BigDecimal(fill.getFillPrice()).subtract(averagePrice);
		BigDecimal qty = new BigDecimal(fill.getQty());
		BigDecimal profit = priceDiff.multiply(qty).divide(new BigDecimal("100"));
		lineNotifyUtils.sendMessage(strategyName, "G1獲利: ", profit.toPlainString());
		log.info("G1獲利: {}", profit.doubleValue());
	}
	
	private void cancelAllG1OpenOrder() {
		for(String orderId : setG1OrderId) {
			if(!cancelOrder(orderId)) {
				log.error("G1刪單失敗: " + orderId);
				lineNotifyUtils.sendMessage("G1刪單失敗:", orderId);
			}
		}
		setG1OrderId.clear();
	}
	
	private void cancelAllG2OpenOrder() {
		for(String orderId : mapG2OrderIdToPrice.keySet()) {
			if(cancelOrder(orderId)) {
				BigDecimal price = mapG2OrderIdToPrice.get(orderId);
				setG2OpenPrice.remove(price.doubleValue());
			} else {
				log.error("G2刪單失敗: " + orderId);
				lineNotifyUtils.sendMessage("G2爆倉刪單失敗");
			}
		}
		mapG2OrderIdToPrice.clear();
	}
	
	private String sendOrder(BuySell buySell, double price, double qty) {
		double orderPrice = PriceUtils.trimPriceWithTicksize(BigDecimal.valueOf(price), tickSize, RoundingMode.UP).doubleValue();
		return exchangeManager.sendOrder(strategyName, ExchangeManager.BTSE_EXCHANGE_NAME, userName, symbol, buySell, orderPrice, qty);
	}
	
	private boolean cancelOrder(String orderId) {
		boolean isSuccess = false;
		if(orderId != null) {
			return exchangeManager.cancelOrder(strategyName, ExchangeManager.BTSE_EXCHANGE_NAME, userName, symbol, orderId);
		}
		return isSuccess;
	}
	
	public static void main(String[] args) {
		Properties prop = System.getProperties();
		prop.setProperty("log4j.configurationFile", "./config/log4j2.xml");
		
		try {
			String configPath = "./config/gigTestnet.json";
			GIGStrategy strategy = new GIGStrategy(new JsonStrategyConfig(configPath));
			strategy.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
