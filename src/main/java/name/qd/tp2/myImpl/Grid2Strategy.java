package name.qd.tp2.myImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
	private double currentFees = 0;

	// 這個策略要用到的值 全部都設定在config
	private int priceRange;
	private int orderSize;
	private String stopProfitType;
	private double stopProfit;
	private double fee;
	private double tickSize;
	private int orderLevel;
	private String lineNotify;
	private double orderStartPrice;
	private int maxContractSize;
	
	private Map<Integer, String> mapPriceToOrderId = new HashMap<>();
	private Map<String, Integer> mapOrderIdToPrice = new HashMap<>();
	private Map<String, Integer> mapStopProfitOrderIdToPrice = new HashMap<>();

	public Grid2Strategy(StrategyConfig strategyConfig) {
		super(strategyConfig);

		// 把設定值從config讀出來
		priceRange = Integer.parseInt(strategyConfig.getCustomizeSettings("priceRange"));
		orderSize = Integer.parseInt(strategyConfig.getCustomizeSettings("orderSize"));
		stopProfitType = strategyConfig.getCustomizeSettings("stopProfitType");
		stopProfit = Double.parseDouble(strategyConfig.getCustomizeSettings("stopProfit"));
		fee = Double.parseDouble(strategyConfig.getCustomizeSettings("fee"));
		tickSize = Double.parseDouble(strategyConfig.getCustomizeSettings("tickSize"));
		orderLevel = Integer.parseInt(strategyConfig.getCustomizeSettings("orderLevel"));
		lineNotify = strategyConfig.getCustomizeSettings("lineNotify");
		orderStartPrice = Double.parseDouble(strategyConfig.getCustomizeSettings("orderStartPrice"));
		maxContractSize = Integer.parseInt(strategyConfig.getCustomizeSettings("maxContractSize"));
		lineNotifyUtils = new LineNotifyUtils(lineNotify);
	}
	
	@Override
	public void strategyAction() {
		// 1. 檢查成交
		//    有成交更新均價
		checkFill();
		
		// 2. 鋪單
		placeOrder();
		
		// 3. 整點回報
		
		// 4. 滿倉回報
	}
	
	private void checkFill() {
		List<Fill> lstFill = exchangeManager.getFill(strategyName, ExchangeManager.BTSE_EXCHANGE_NAME);
		for(Fill fill : lstFill) {
			String orderId = fill.getOrderId();
			if(mapOrderIdToPrice.containsKey(orderId)) {
				// 開倉單成交
			} else if(mapStopProfitOrderIdToPrice.containsKey(orderId)) {
				
			} else {
				log.error("未知成交: {}", orderId);
			}
		}
	}
	
	private void placeOrder() {
		Orderbook orderbook = exchangeManager.getOrderbook(ExchangeManager.BTSE_EXCHANGE_NAME, symbol);
		if(orderbook == null) return;
		
		double orderPrice = BigDecimal.valueOf(orderbook.getBidTopPrice(1)[0]).setScale(0, RoundingMode.DOWN).doubleValue();

		for(int i = 0 ; i < orderLevel ;) {
			String orderId = sendOrder(BuySell.BUY, orderPrice - i, orderSize);
			if(orderId != null) {
				mapOrderIdToPrice.put(orderId, (int) orderPrice - i);
				i++;
			}
		}
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
			String configPath = "./config/grid2.json";
			Grid2Strategy strategy = new Grid2Strategy(new JsonStrategyConfig(configPath));
			strategy.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
