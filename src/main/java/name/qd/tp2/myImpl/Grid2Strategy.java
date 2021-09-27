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
	private String symbol = "ETHPFC";
	private int position = 0;
	private double averagePrice = 0;
	private double cost = 0;
	
	private int buyCount = 0;
	private int sellCount = 0;

	// 這個策略要用到的值 全部都設定在config
	private int priceRange;
	private int orderSize;
	private StopProfitType stopProfitType;
	private BigDecimal stopProfit;
	private BigDecimal tickSize;
	private int orderLevel;
	private String lineNotify;
	private int reportMinute;
	private BigDecimal ceilingPrice;
	private BigDecimal floorPrice;
	private int notifyMinute = -1;
	
	private List<BigDecimal> lstBuy = new ArrayList<>();
	private List<BigDecimal> lstSell = new ArrayList<>();
	
	private boolean pause = false;
	private boolean start = false;
	private boolean placeFirst = true;
	private long lastFillTime;
	
	private Map<String, BigDecimal> mapOrderIdToPrice = new HashMap<>();
	private Set<Double> setOpenPrice = new HashSet<>();
	private Map<String, BigDecimal> mapStopProfitOrderId = new HashMap<>();

	public Grid2Strategy(StrategyConfig strategyConfig) {
		super(strategyConfig);

		// 把設定值從config讀出來
		priceRange = Integer.parseInt(strategyConfig.getCustomizeSettings("priceRange"));
		orderSize = Integer.parseInt(strategyConfig.getCustomizeSettings("orderSize"));
		stopProfitType = StopProfitType.valueOf(strategyConfig.getCustomizeSettings("stopProfitType"));
		stopProfit = new BigDecimal(strategyConfig.getCustomizeSettings("stopProfit"));
		tickSize = new BigDecimal(strategyConfig.getCustomizeSettings("fee"));
		orderLevel = Integer.parseInt(strategyConfig.getCustomizeSettings("orderLevel"));
		lineNotify = strategyConfig.getCustomizeSettings("lineNotify");
		lineNotifyUtils = new LineNotifyUtils(lineNotify);
		reportMinute = Integer.parseInt(strategyConfig.getCustomizeSettings("reportMinute"));
		ceilingPrice = new BigDecimal(strategyConfig.getCustomizeSettings("ceilingPrice"));
		floorPrice = new BigDecimal(strategyConfig.getCustomizeSettings("floorPrice"));
	}
	
	@Override
	public void strategyAction() {
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
//		checkPosition();
		
		// 5. 刪掉太遠的買單
		closeFarOpenOrder();
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
		List<Fill> lstFill = exchangeManager.getFill(strategyName, ExchangeManager.BTSE_EXCHANGE);
		processFill(lstFill);
	}
	
	private void checkFillFromApi() {
		// websocket 不可靠 打API拿成交
		ZonedDateTime zonedDateTime = ZonedDateTime.now();
		long to = zonedDateTime.toEpochSecond() * 1000;
		
		List<Fill> lst = exchangeManager.getFillHistory(ExchangeManager.BTSE_EXCHANGE, userName, symbol, lastFillTime, to);
		if(lst == null) return;
		
		lastFillTime = to;
		
		processFill(lst);
	}
	
	private void processFill(List<Fill> lstFill) {
		for(Fill fill : lstFill) {
			String orderId = fill.getOrderId();
			if(mapOrderIdToPrice.containsKey(orderId)) {
				// 開倉單成交 下停利單
				BigDecimal price = mapOrderIdToPrice.remove(orderId);
				placeStopProfitOrder(PriceUtils.getStopProfitPrice(price, stopProfitType, stopProfit));
				
				log.info("開倉單成交: {} {} {}, 下對應停利: {}", fill.getBuySell(), fill.getFillPrice(), fill.getQty(), PriceUtils.getStopProfitPrice(price, stopProfitType, stopProfit));
				buyCount++;
				// 算均價
				calcAvgPrice(fill);
			} else if(mapStopProfitOrderId.containsKey(orderId)) {
				// 停利單成交
				BigDecimal price = mapStopProfitOrderId.remove(orderId);
				// TODO 目前回算open order價格是fix方式
				setOpenPrice.remove(price.subtract(stopProfit).doubleValue());
				
				log.info("停利單成交: {} {} {}, 對應開倉應於: {}", fill.getBuySell(), fill.getFillPrice(), fill.getQty(), price.subtract(stopProfit));
				sellCount++;
				// 算均價
				calcAvgPrice(fill);
			} else {
				log.error("未知成交: {}", orderId);
			}
		}
	}
	
	private void placeOrder() {
		Orderbook orderbook = exchangeManager.getOrderbook(ExchangeManager.BTSE_EXCHANGE, symbol);
		if(orderbook == null) return;

		int orderPrice = BigDecimal.valueOf(orderbook.getBidTopPrice(1)[0]).setScale(0, RoundingMode.DOWN).intValue();
		
		if(orderPrice - stopProfit.intValue() > ceilingPrice.intValue()) {
//			log.info("價格已到天花板 {} {}", orderPrice, ceilingPrice.toPlainString());
			return;
		} else if(orderPrice < floorPrice.intValue()) {
//			log.info("價格已到地板 {} {}", orderPrice, floorPrice.toPlainString());
			return;
		}
		
		if(placeFirst) {
			orderPrice -= orderPrice % priceRange;
			placeFirst = false;
		} else {
			orderPrice -= orderPrice % priceRange;
			orderPrice -= priceRange;
		}
		
		for(int i = 0 ; i < orderLevel ;) {
			BigDecimal price = BigDecimal.valueOf(orderPrice - (i * priceRange));
			if(!setOpenPrice.contains(price.doubleValue())) {
				String orderId = sendOrder(BuySell.BUY, price.doubleValue(), orderSize);
				if(orderId != null) {
					mapOrderIdToPrice.put(orderId, price);
					setOpenPrice.add(price.doubleValue());
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
	
	private void closeFarOpenOrder() {
		if(setOpenPrice.size() > orderLevel + 1) {
			Collection<BigDecimal> collection = mapOrderIdToPrice.values();
			List<BigDecimal> lst = new ArrayList<>(collection);
			Collections.sort(lst);
			
			List<String> lstRemoveOrderId = new ArrayList<>();
			for(int i = 0 ; i < lst.size() - orderLevel - 1 ; i++) {
				int index = i;
				mapOrderIdToPrice.forEach((orderId, price) -> {
					if(price.equals(lst.get(index))) {
						lstRemoveOrderId.add(orderId);
					}
				});
			}
			
			for(String orderId : lstRemoveOrderId) {
				cancelOrder(orderId);
				BigDecimal price = mapOrderIdToPrice.remove(orderId);
				setOpenPrice.remove(price.doubleValue());
			}
		}
		
	}
	
	private void cancelAllOpenOrder() {
		for(String orderId : mapOrderIdToPrice.keySet()) {
			if(cancelOrder(orderId)) {
				BigDecimal price = mapOrderIdToPrice.get(orderId);
				setOpenPrice.remove(price.doubleValue());
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
			cost = cost + (qty * fillPrice) + Double.valueOf(fill.getFee());
			averagePrice = cost / position;
		} else {
			position -= qty;
			cost = cost - (qty * fillPrice) + Double.valueOf(fill.getFee());
			averagePrice = cost / position;
		}
		log.info("position: {}, cost: {}, avgPrice: {}", position, cost, averagePrice);
	}
	
	private String sendOrder(BuySell buySell, double price, double qty) {
		log.info("send order: {} {} {}", buySell, price, qty);
		return exchangeManager.sendOrder(strategyName, ExchangeManager.BTSE_EXCHANGE, userName, symbol, buySell, price, qty);
	}
	
	private boolean cancelOrder(String orderId) {
		log.info("cancel order: {}", orderId);
		return exchangeManager.cancelOrder(strategyName, ExchangeManager.BTSE_EXCHANGE, userName, symbol, orderId);
	}
	
	public static void main(String[] s) {
		Properties prop = System.getProperties();
		prop.setProperty("log4j.configurationFile", "./config/log4j2.xml");

		try {
//			String configPath = "./config/grid2.json";
			String configPath = "./config/grid2testnet.json";
			Grid2Strategy strategy = new Grid2Strategy(new JsonStrategyConfig(configPath));
			strategy.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
