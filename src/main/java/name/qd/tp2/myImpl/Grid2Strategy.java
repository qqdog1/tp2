package name.qd.tp2.myImpl;

import java.io.IOException;
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

import name.qd.fileCache.FileCacheManager;
import name.qd.fileCache.cache.NormalCacheManager;
import name.qd.fileCache.cache.NormalObject;
import name.qd.tp2.constants.BuySell;
import name.qd.tp2.constants.StopProfitType;
import name.qd.tp2.exchanges.ExchangeManager;
import name.qd.tp2.exchanges.vo.Fill;
import name.qd.tp2.exchanges.vo.Orderbook;
import name.qd.tp2.myImpl.vo.FillCacheByPrice;
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
	
	private long from;
	
	private Map<String, BigDecimal> mapOrderIdToPrice = new HashMap<>();
	private Set<Double> setOpenPrice = new HashSet<>();
	private Map<String, BigDecimal> mapStopProfitOrderId = new HashMap<>();
	
	private FileCacheManager fileCacheManager;
	private NormalCacheManager grid2CacheManager;
	
	private String tradingExchange;

	public Grid2Strategy(StrategyConfig strategyConfig) {
		super(strategyConfig);

		// 把設定值從config讀出來
		priceRange = Integer.parseInt(strategyConfig.getCustomizeSettings("priceRange"));
		orderSize = Integer.parseInt(strategyConfig.getCustomizeSettings("orderSize"));
		stopProfitType = StopProfitType.valueOf(strategyConfig.getCustomizeSettings("stopProfitType"));
		stopProfit = new BigDecimal(strategyConfig.getCustomizeSettings("stopProfit"));
		tickSize = new BigDecimal(strategyConfig.getCustomizeSettings("tickSize"));
		orderLevel = Integer.parseInt(strategyConfig.getCustomizeSettings("orderLevel"));
		lineNotify = strategyConfig.getCustomizeSettings("lineNotify");
		lineNotifyUtils = new LineNotifyUtils(lineNotify);
		reportMinute = Integer.parseInt(strategyConfig.getCustomizeSettings("reportMinute"));
		ceilingPrice = new BigDecimal(strategyConfig.getCustomizeSettings("ceilingPrice"));
		floorPrice = new BigDecimal(strategyConfig.getCustomizeSettings("floorPrice"));
		tradingExchange = strategyConfig.getCustomizeSettings("tradingExchange");
	
		ZonedDateTime zonedDateTime = ZonedDateTime.now();
		from = zonedDateTime.toEpochSecond() * 1000;
		
		fileCacheManager = new FileCacheManager("./grid2");
		
		initAndRestoreCache();
	}
	
	private void initAndRestoreCache() {
		try {
			grid2CacheManager = fileCacheManager.getNormalCacheInstance("grid2", FillCacheByPrice.class.getName());
		} catch (Exception e) {
			log.error("init cache failed", e);
			lineNotifyUtils.sendMessage(strategyName, "init cache failed");
		}
		
		for(NormalObject object : grid2CacheManager.values()) {
			FillCacheByPrice fill = (FillCacheByPrice) object;
			
			log.info("未平倉 {} {} {} {}", fill.getBuySell(), fill.getOrderPrice(), fill.getQty(), fill.getOrderId());
			
			if(fill.getBuySell() == BuySell.BUY) {
				double price = Double.parseDouble(fill.getOrderPrice());
				setOpenPrice.add(price);
				placeStopProfitOrder(PriceUtils.getStopProfitPrice(BigDecimal.valueOf(price), stopProfitType, stopProfit));
				
				calcAvgPrice(fill.getFill());
			} else {
				log.error("未平倉不應有賣單");
			}
		}
	}
	
	@Override
	public void strategyAction() {
		// 1. 檢查成交
		//    有成交更新均價
		checkFill();
//		checkFillFromApi();
		
		// 2. 鋪單
		placeOrder();
		
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
		} else {
			log.info("下停利單 {} {} {}", price, orderSize, stopOrderId);
		}
		mapStopProfitOrderId.put(stopOrderId, price);
	}
	
	private void checkFill() {
		// TODO partial fill
		// partial fill 應該就是成交多少 就下多少的反向就可以?
		// 所以當有一單被切過部分成交 後續就一直都是碎片單在下
		// EX: 本來一筆單是2 contracts
		//     下B 2 contracts
		//     fill B 1 contract
		//     下S 1 contract
		//     fill B 1 contract
		//     下S 1 contract
		//     此時S單不管怎樣都是兩單
		//     程式後續怎麼跑都會變兩單在跑
		//     worst case 所有單都變1contract 全部當taker 沒有手續費優惠
		List<Fill> lstFill = exchangeManager.getFill(strategyName, tradingExchange);
		processFill(lstFill);
	}
	
//	private void checkFillFromApi() {
//		// websocket 不可靠 打API拿成交
//		ZonedDateTime zonedDateTime = ZonedDateTime.now();
//		long to = zonedDateTime.toEpochSecond() * 1000;
//		
//		List<Fill> lst = exchangeManager.getFillHistory(tradingExchange, userName, symbol, from, to);
//		if(lst == null) return;
//		
//		from = to;
//		
//		processFill(lst);
//	}
	
	private void processFill(List<Fill> lstFill) {
		for(Fill fill : lstFill) {
			String orderId = fill.getOrderId();
			if(mapOrderIdToPrice.containsKey(orderId)) {
				// 開倉單成交 下停利單
				BigDecimal price = mapOrderIdToPrice.remove(orderId);
				placeStopProfitOrder(PriceUtils.getStopProfitPrice(price, stopProfitType, stopProfit));
				
				log.info("開倉單成交: {} {} {} {}, 下對應停利: {}", fill.getBuySell(), fill.getFillPrice(), fill.getQty(), orderId, PriceUtils.getStopProfitPrice(price, stopProfitType, stopProfit));
				buyCount++;
				// 算均價
				calcAvgPrice(fill);
				
				addFillToCache(fill);
			} else if(mapStopProfitOrderId.containsKey(orderId)) {
				// 停利單成交
				BigDecimal price = mapStopProfitOrderId.remove(orderId);
				// TODO 目前回算open order價格是fix方式
				double removePrice = price.subtract(stopProfit).doubleValue();
				setOpenPrice.remove(removePrice);
				
				log.info("停利單成交: {} {} {}, 對應開倉應於: {}", fill.getBuySell(), fill.getFillPrice(), fill.getQty(), price.subtract(stopProfit));
				sellCount++;
				// 算均價
				calcAvgPrice(fill);
				
				removeFillFromCache(removePrice);
			} else {
				log.error("未知成交: {}", orderId);
			}
		}
	}
	
	private void addFillToCache(Fill fill) {
		FillCacheByPrice fillCacheByPrice = new FillCacheByPrice(fill);
		grid2CacheManager.put(fillCacheByPrice);
		
		try {
			grid2CacheManager.writeCacheToFile();
		} catch (IOException e) {
			log.error("write cache to file failed.", e);
		}
	}
	
	private void removeFillFromCache(double removePrice) {
		grid2CacheManager.remove(String.valueOf(removePrice));
		
		try {
			grid2CacheManager.writeCacheToFile();
		} catch (IOException e) {
			log.error("write cache to file failed.", e);
		}
	}
	
	private void placeOrder() {
		Orderbook orderbook = exchangeManager.getOrderbook(tradingExchange, symbol);
		if(orderbook == null) return;

		int orderPrice = BigDecimal.valueOf(orderbook.getBidTopPrice(1)[0]).setScale(0, RoundingMode.DOWN).intValue();
		orderPrice -= orderPrice % priceRange;
		
		for(int i = 0 ; i < orderLevel ;) {
			BigDecimal price = BigDecimal.valueOf(orderPrice - (i * priceRange));
			
			if(price.doubleValue() > ceilingPrice.intValue()) {
//				log.info("價格已到天花板 {} {}", price.toPlainString(), ceilingPrice.toPlainString());
				i++;
				continue;
			} else if(price.doubleValue() < floorPrice.intValue()) {
//				log.info("價格已到地板 {} {}", price.toPlainString(), floorPrice.toPlainString());
				return;
			}
			
			if(!setOpenPrice.contains(price.doubleValue())) {
				String orderId = sendOrder(BuySell.BUY, price.doubleValue(), orderSize);
				if(orderId != null) {
					mapOrderIdToPrice.put(orderId, price);
					setOpenPrice.add(price.doubleValue());
					i++;
					log.info("下開倉單: {} {} {}", price, orderSize, orderId);
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
		if(setOpenPrice.size() > orderLevel) {
			Collection<BigDecimal> collection = mapOrderIdToPrice.values();
			List<BigDecimal> lst = new ArrayList<>(collection);
			Collections.sort(lst);
			
			List<String> lstRemoveOrderId = new ArrayList<>();
			for(int i = 0 ; i < lst.size() - orderLevel ; i++) {
				int index = i;
				mapOrderIdToPrice.forEach((orderId, price) -> {
					if(price.equals(lst.get(index))) {
						lstRemoveOrderId.add(orderId);
					}
				});
			}
			
			for(String orderId : lstRemoveOrderId) {
				if(cancelOrder(orderId)) {
					BigDecimal price = mapOrderIdToPrice.remove(orderId);
					setOpenPrice.remove(price.doubleValue());
					log.info("刪除距離太遠開倉單: {} {} {}", price, orderSize, orderId);
				}
			}
		}
	}
	
	private void calcAvgPrice(Fill fill) {
		int qty = (int) Double.parseDouble(fill.getQty());
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
		BigDecimal orderPrice = PriceUtils.trimPriceWithTicksize(BigDecimal.valueOf(price), tickSize, RoundingMode.UP);
		log.info("send order: {} {} {}", buySell, price, qty);
		return exchangeManager.sendOrder(strategyName, tradingExchange, userName, symbol, buySell, orderPrice.doubleValue(), qty);
	}
	
	private boolean cancelOrder(String orderId) {
		log.info("cancel order: {}", orderId);
		return exchangeManager.cancelOrder(strategyName, tradingExchange, userName, symbol, orderId);
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
