package name.qd.tp2.myImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.qd.tp2.constants.BuySell;
import name.qd.tp2.exchanges.ExchangeManager;
import name.qd.tp2.exchanges.vo.Fill;
import name.qd.tp2.exchanges.vo.Orderbook;
import name.qd.tp2.strategies.AbstractStrategy;
import name.qd.tp2.strategies.config.JsonStrategyConfig;
import name.qd.tp2.strategies.config.StrategyConfig;
import name.qd.tp2.utils.LineNotifyUtils;

/**
	進場: 現價向下吸單 沒跌N元吸M
	出場: 吸到直接掛向上X元停利單
	其他: 價格向上手上沒單
	     價格向下吸一堆
	     定時回報當下持倉量及成本
	     預期成本要低於現價
	     每次持倉歸零時就是獲利
	     達到max contract size停止策略下均價以上固定點數平倉單
*/
public class Grid2Strategy extends AbstractStrategy {
	private Logger log = LoggerFactory.getLogger(Grid2Strategy.class);
	private LineNotifyUtils lineNotifyUtils;
	// 與交易所溝通用
	private ExchangeManager exchangeManager = ExchangeManager.getInstance();

	private String strategyName = "[上下爆吸]";
	private String userName = "shawn";

	// 自己設定一些策略內要用的變數
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private String symbol = "ETHPFC";
	private int position = 0;
	private double averagePrice = 0;
	private double cost = 0;
	
	private int buyCount = 0;
	private int sellCount = 0;

	// 這個策略要用到的值 全部都設定在config
	private int priceRange;
	private int orderSize;
	private String stopProfitType;
	private double stopProfit;
	private double fee;
	private int orderLevel;
	private String lineNotify;
	private int maxContractSize;
	private int reportMinute;
	private int notifyMinute = -1;
	private String startTime;
	
	private List<BigDecimal> lstBuy = new ArrayList<>();
	private List<BigDecimal> lstSell = new ArrayList<>();
	
	private boolean pause = false;
	private boolean start = false;
	private long lastFillTime;
	
	private Map<String, BigDecimal> mapOrderIdToPrice = new HashMap<>();
	private Set<BigDecimal> setOpenPrice = new HashSet<>();
	private Map<String, BigDecimal> mapStopProfitOrderId = new HashMap<>();

	public Grid2Strategy(StrategyConfig strategyConfig) {
		super(strategyConfig);

		// 把設定值從config讀出來
		startTime = strategyConfig.getCustomizeSettings("startTime");
		priceRange = Integer.parseInt(strategyConfig.getCustomizeSettings("priceRange"));
		orderSize = Integer.parseInt(strategyConfig.getCustomizeSettings("orderSize"));
		stopProfitType = strategyConfig.getCustomizeSettings("stopProfitType");
		stopProfit = Double.parseDouble(strategyConfig.getCustomizeSettings("stopProfit"));
		fee = Double.parseDouble(strategyConfig.getCustomizeSettings("fee"));
		orderLevel = Integer.parseInt(strategyConfig.getCustomizeSettings("orderLevel"));
		lineNotify = strategyConfig.getCustomizeSettings("lineNotify");
		maxContractSize = Integer.parseInt(strategyConfig.getCustomizeSettings("maxContractSize"));
		lineNotifyUtils = new LineNotifyUtils(lineNotify);
		reportMinute = Integer.parseInt(strategyConfig.getCustomizeSettings("reportMinute"));
	}
	
	@Override
	public void strategyAction() {
		// 0. 依照歷史成交紀錄推斷上次沒結束的交易
		if(!start) {
			checkRemainOrder();
			start = true;
		}
		
		// 1. 檢查成交
		//    有成交更新均價
//		checkFill();
		checkFillFromApi();
		
		// 2. 鋪單
		if(!pause) {
			placeOrder();
		}
		
		// 3. 定時回報
		report();
		
		// 4. 滿倉回報
		checkPosition();
	}
	
	private void checkRemainOrder() {
		if(startTime != null) {
			Date date = null;
			try {
				sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
				date = sdf.parse(startTime);
			} catch (ParseException e) {
				log.error("parse date failed.", e);
			}
			
			long from = date.getTime();
			ZonedDateTime zonedDateTime = ZonedDateTime.now();
			long to = zonedDateTime.toEpochSecond() * 1000;
			lastFillTime = to;
			
			boolean isLast = false;
			while(!isLast) {
				List<Fill> lst = exchangeManager.getFillHistory(ExchangeManager.BTSE_EXCHANGE_NAME, userName, symbol, from, to);
				log.info("get fill size: {}", lst.size());
				for(Fill fill : lst) {
					int qty = Integer.parseInt(fill.getQty());
					if(qty == 1) {
						calcAvgPrice(fill);
						BigDecimal price = new BigDecimal(fill.getOrderPrice());
						if(fill.getBuySell() == BuySell.BUY) {
							if(lstSell.contains(price.add(BigDecimal.valueOf(stopProfit)))) {
								if(!lstSell.remove(price.add(BigDecimal.valueOf(stopProfit)))) {
									log.error("刪除cache失敗");
								}
							} else {
								lstBuy.add(price);
							}
						} else if(fill.getBuySell() == BuySell.SELL) {
							if(lstBuy.contains(price.subtract(BigDecimal.valueOf(stopProfit)))) {
								if(!lstBuy.remove(price.subtract(BigDecimal.valueOf(stopProfit)))) {
									log.error("刪除cache失敗");
								}
							} else {
								lstSell.add(price);
							}
						} else {
							log.error("unknown side");
						}
					}
					to = fill.getTimestamp();
				}
				
				if(lst.size() < 100) {
					isLast = true;
				}
			}
			
			// 補單
			// 遺留買單成交 補停利單 補cache
			lstBuy.forEach((price) -> {
				// 避免再下買單鋪單
				setOpenPrice.add(price);
				// 下停利單
				placeStopProfitOrder(getStopProfitPrice(price));
//				log.info("鋪賣 {}", getStopProfitPrice(price));
				
			});
			
			// 遺留賣單成交 補開倉單
			// 會沒算到成本 直到賣單清零重開才會算對
			lstSell.forEach((price) -> {
				sendOrder(BuySell.BUY, price.subtract(BigDecimal.valueOf(stopProfit)).doubleValue(), orderSize);
//				log.info("鋪買 {}", price.subtract(BigDecimal.valueOf(stopProfit)).doubleValue());
			});
		}
	}
	
	private void placeStopProfitOrder(BigDecimal price) {
		String stopOrderId = sendOrder(BuySell.SELL, price.doubleValue(), orderSize);
		if(stopOrderId == null) {
			log.error("下停利單失敗");
			lineNotifyUtils.sendMessage(strategyName + " 下停利單失敗: " + price);
		}
		mapStopProfitOrderId.put(stopOrderId, price);
	}
	
	private void checkFill() {
		// TODO partial fill
		List<Fill> lstFill = exchangeManager.getFill(strategyName, ExchangeManager.BTSE_EXCHANGE_NAME);
		processFill(lstFill);
	}
	
	private void checkFillFromApi() {
		// websocket 不可靠 打API拿成交
		ZonedDateTime zonedDateTime = ZonedDateTime.now();
		long to = zonedDateTime.toEpochSecond() * 1000;
		
		List<Fill> lst = exchangeManager.getFillHistory(ExchangeManager.BTSE_EXCHANGE_NAME, userName, symbol, lastFillTime, to);
		lastFillTime = to;
		
		processFill(lst);
	}
	
	private void processFill(List<Fill> lstFill) {
		for(Fill fill : lstFill) {
			String orderId = fill.getOrderId();
			if(mapOrderIdToPrice.containsKey(orderId)) {
				// 開倉單成交 下停利單
				BigDecimal price = mapOrderIdToPrice.remove(orderId);
				placeStopProfitOrder(getStopProfitPrice(price));
				
				log.info("開倉單成交: {} {} {}, 下對應停利: {}", fill.getBuySell(), fill.getFillPrice(), fill.getQty(), getStopProfitPrice(price));
				buyCount++;
				// 算均價
				calcAvgPrice(fill);
			} else if(mapStopProfitOrderId.containsKey(orderId)) {
				// 停利單成交
				BigDecimal price = mapStopProfitOrderId.remove(orderId);
				// TODO 目前回算open order價格是fix方式
				setOpenPrice.remove(price.subtract(BigDecimal.valueOf(stopProfit)));
				
				log.info("停利單成交: {} {} {}, 對應開倉應於: {}", fill.getBuySell(), fill.getFillPrice(), fill.getQty(), price.subtract(BigDecimal.valueOf(stopProfit)));
				sellCount++;
				// 算均價
				calcAvgPrice(fill);
			} else {
				log.error("未知成交: {}", orderId);
			}
		}
	}
	
	private BigDecimal getStopProfitPrice(BigDecimal price) {
		// TODO 目前只能用fix
		// TODO 要加入手續費計算 不然價格大fix加上去可能還cover不掉手續費
		if("fix".equals(stopProfitType)) {
			return price.add(BigDecimal.valueOf(stopProfit));
//		} else if("rate".equals(stopProfitType)) {
//			return (int) (price * stopProfit);
		} else {
			log.error("未知停利方式...");
			return BigDecimal.ZERO;
		}
	}
	
	private void placeOrder() {
		Orderbook orderbook = exchangeManager.getOrderbook(ExchangeManager.BTSE_EXCHANGE_NAME, symbol);
		if(orderbook == null) return;

		int orderPrice = BigDecimal.valueOf(orderbook.getBidTopPrice(1)[0]).setScale(0, RoundingMode.DOWN).intValue();
		orderPrice -= orderPrice % priceRange;
		
		for(int i = 0 ; i < orderLevel ;) {
			BigDecimal price = BigDecimal.valueOf(orderPrice - (i * priceRange));
			if(!setOpenPrice.contains(price)) {
				String orderId = sendOrder(BuySell.BUY, price.doubleValue(), orderSize);
				if(orderId != null) {
					mapOrderIdToPrice.put(orderId, price);
					setOpenPrice.add(price);
					i++;
				} 
			} else {
				i++;
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
	
	private void checkPosition() {
		if(pause) {
			if(position < maxContractSize) {
				pause = false;
			}
		} else {
			if(position >= maxContractSize) {
				lineNotifyUtils.sendMessage(strategyName + "爆倉中, 注意策略狀態");
				cancelAllOpenOrder();
				pause = true;
			}
		}
	}
	
	private void cancelAllOpenOrder() {
		for(String orderId : mapOrderIdToPrice.keySet()) {
			if(cancelOrder(orderId)) {
				BigDecimal price = mapOrderIdToPrice.get(orderId);
				setOpenPrice.remove(price);
			} else {
				log.error("刪單失敗: " + orderId);
				lineNotifyUtils.sendMessage("爆倉刪單失敗");
			}
		}
		mapOrderIdToPrice.clear();
	}
	
	private void calcAvgPrice(Fill fill) {
		int qty = Integer.parseInt(fill.getQty());
		double fillPrice = Double.parseDouble(fill.getFillPrice());
		if(BuySell.BUY == fill.getBuySell()) {
			position += qty;
			double feeCost = qty * fillPrice * fee;
			cost = cost + (qty * fillPrice) + feeCost;
			averagePrice = cost / position;
		} else {
			position -= qty;
			double feeCost = qty * fillPrice * fee;
			cost = cost - (qty * fillPrice) + feeCost;
			averagePrice = cost / position;
		}
		log.info("position: {}, cost: {}, avgPrice: {}", position, cost, averagePrice);
	}
	
	private String sendOrder(BuySell buySell, double price, double qty) {
		log.info("send order: {} {} {}", buySell, price, qty);
		return exchangeManager.sendOrder(strategyName, ExchangeManager.BTSE_EXCHANGE_NAME, userName, symbol, buySell, price, qty);
	}
	
	private boolean cancelOrder(String orderId) {
		log.info("cancel order: {}", orderId);
		return exchangeManager.cancelOrder(strategyName, ExchangeManager.BTSE_EXCHANGE_NAME, userName, symbol, orderId);
	}
	
	public static void main(String[] s) {
		Properties prop = System.getProperties();
		prop.setProperty("log4j.configurationFile", "./config/log4j2.xml");

		try {
			String configPath = "./config/grid2.json";
//			String configPath = "./config/grid2testnet.json";
			Grid2Strategy strategy = new Grid2Strategy(new JsonStrategyConfig(configPath));
			strategy.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
