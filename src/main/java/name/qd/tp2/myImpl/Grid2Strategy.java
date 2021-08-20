package name.qd.tp2.myImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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
	private String symbol = "ETHPFC";
	private int position = 0;
	private double averagePrice = 0;
	private double cost = 0;

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
	
	private boolean pause = false;
	
	private Map<String, Integer> mapOrderIdToPrice = new HashMap<>();
	private Set<Integer> setOpenPrice = new HashSet<>();
	private Map<String, Integer> mapStopProfitOrderId = new HashMap<>();

	public Grid2Strategy(StrategyConfig strategyConfig) {
		super(strategyConfig);

		// 把設定值從config讀出來
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
		// 1. 檢查成交
		//    有成交更新均價
		checkFill();
		
		// 2. 鋪單
		if(!pause) {
			placeOrder();
		}
		
		// 3. 定時回報
		report();
		
		// 4. 滿倉回報
		checkPosition();
	}
	
	private void checkFill() {
		// TODO partial fill
		List<Fill> lstFill = exchangeManager.getFill(strategyName, ExchangeManager.BTSE_EXCHANGE_NAME);
		for(Fill fill : lstFill) {
			String orderId = fill.getOrderId();
			if(mapOrderIdToPrice.containsKey(orderId)) {
				// 開倉單成交 下停利單
				Integer price = mapOrderIdToPrice.remove(orderId);
				price = getStopProfitPrice(price);
				String stopOrderId = sendOrder(BuySell.SELL, price, orderSize);
				mapStopProfitOrderId.put(stopOrderId, price);
				
				log.info("fill: {} {} {}", fill.getBuySell(), fill.getPrice(), fill.getQty());
				// 算均價
				calcAvgPrice(fill);
			} else if(mapStopProfitOrderId.containsKey(orderId)) {
				// 停利單成交
				Integer price = mapStopProfitOrderId.remove(orderId);
				// TODO 目前回算open order價格是fix方式
				setOpenPrice.remove(price - (int) stopProfit);
				
				log.info("fill: {} {} {}", fill.getBuySell(), fill.getPrice(), fill.getQty());
				// 算均價
				calcAvgPrice(fill);
			} else {
				log.error("未知成交: {}", orderId);
			}
		}
	}
	
	private int getStopProfitPrice(int price) {
		// TODO 目前只能用fix
		if("fix".equals(stopProfitType)) {
			return price + (int) stopProfit;
//		} else if("rate".equals(stopProfitType)) {
//			return (int) (price * stopProfit);
		} else {
			log.error("未知停利方式...");
			return 0;
		}
	}
	
	private void placeOrder() {
		Orderbook orderbook = exchangeManager.getOrderbook(ExchangeManager.BTSE_EXCHANGE_NAME, symbol);
		if(orderbook == null) return;
		
		int orderPrice = BigDecimal.valueOf(orderbook.getBidTopPrice(1)[0]).setScale(0, RoundingMode.DOWN).intValue();
		orderPrice -= stopProfit;
		
		for(int i = 0 ; i < orderLevel ;) {
			int price = orderPrice - (i * priceRange);
			
			if(!setOpenPrice.contains(price)) {
				String orderId = sendOrder(BuySell.BUY, price, orderSize);
				if(orderId != null) {
					mapOrderIdToPrice.put(orderId, (int) price);
					setOpenPrice.add(price);
					i++;
				} 
			}
		}
	}
	
	private void report() {
		ZonedDateTime zonedDateTime = ZonedDateTime.now();
		int minute = zonedDateTime.getMinute();
		if(minute % reportMinute == 0) {
			lineNotifyUtils.sendMessage(strategyName + "現在持倉: " + position + " 平均成本: " + averagePrice);
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
				Integer price = mapOrderIdToPrice.remove(orderId);
				setOpenPrice.remove(price);
			} else {
				log.error("刪單失敗: " + orderId);
			}
		}
	}
	
	private void calcAvgPrice(Fill fill) {
		if(BuySell.BUY == fill.getBuySell()) {
			position += fill.getQty();
			double feeCost = fill.getQty() * fill.getPrice() * fee;
			cost = cost + (fill.getQty() * fill.getPrice()) + feeCost;
			averagePrice = cost / position;
		} else {
			position -= fill.getQty();
			double feeCost = fill.getQty() * fill.getPrice() * fee;
			cost = cost - (fill.getQty() * fill.getPrice()) + feeCost;
			averagePrice = cost / position;
		}
		log.info("position: {}, cost: {}, avgPrice: {}", position, cost, averagePrice);
	}
	
	private String sendOrder(BuySell buySell, double price, double qty) {
		return exchangeManager.sendOrder(strategyName, ExchangeManager.BTSE_EXCHANGE_NAME, userName, symbol, buySell, price, qty);
	}
	
	private boolean cancelOrder(String orderId) {
		return exchangeManager.cancelOrder(strategyName, ExchangeManager.BTSE_EXCHANGE_NAME, userName, symbol, orderId);
	}
	
	public static void main(String[] s) {
		Properties prop = System.getProperties();
		prop.setProperty("log4j.configurationFile", "./config/log4j2.xml");

		try {
			String configPath = "./config/grid2testnet.json";
			Grid2Strategy strategy = new Grid2Strategy(new JsonStrategyConfig(configPath));
			strategy.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
