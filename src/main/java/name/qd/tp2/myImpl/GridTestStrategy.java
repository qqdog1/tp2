package name.qd.tp2.myImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.qd.tp2.constants.BuySell;
import name.qd.tp2.exchanges.ExchangeManager;
import name.qd.tp2.exchanges.vo.Fill;
import name.qd.tp2.exchanges.vo.Orderbook;
import name.qd.tp2.strategies.AbstractStrategy;
import name.qd.tp2.strategies.Strategy;
import name.qd.tp2.strategies.config.JsonStrategyConfig;
import name.qd.tp2.strategies.config.StrategyConfig;

// 自行實作策略一定要繼承AbstractStrategy
public class GridTestStrategy extends AbstractStrategy {
	private Logger log = LoggerFactory.getLogger(GridTestStrategy.class);
	// 與交易所溝通用
	private ExchangeManager exchangeManager = ExchangeManager.getInstance();
	
	// 自己設定一些策略內要用的變數
	private int position = 0;
	private double averagePrice = 0;
	private double targetPrice = 0;
	private double currentFees = 0;
	
	// 這個策略要用到的值 全部都設定在config
	private int priceRange;
	private int firstContractSize;
	private double stopProfit;
	private double fee;
	private double tickSize;
	private int orderLevel;
	
	// 
	private List<Double> priceLevel;
	private int currentLevel;
	
	// 給strategy一個名字 當同時跑多個策略時 才分的出是誰下的單
	private static String strategyName = "test111";
	private static String userName = "shawn";

	public GridTestStrategy(StrategyConfig strategyConfig) {
		super(strategyConfig);
		
		// 把設定值從config讀出來
		priceRange = Integer.parseInt(strategyConfig.getCustomizeSettings("priceRange"));
		firstContractSize = Integer.parseInt(strategyConfig.getCustomizeSettings("firstContractSize"));
		stopProfit = Double.parseDouble(strategyConfig.getCustomizeSettings("stopProfit")) / 100 + 1;
		fee = Double.parseDouble(strategyConfig.getCustomizeSettings("fee"));
		tickSize = Double.parseDouble(strategyConfig.getCustomizeSettings("tickSize"));
		orderLevel = Integer.parseInt(strategyConfig.getCustomizeSettings("orderLevel"));
		priceLevel = new ArrayList<>();
	}

	// 這個Method會以一個獨立的thread無限迴圈執行
	@Override
	public void strategyAction() {
		// 取得未處裡的成交
//		List<Fill> lstFill = exchangeManager.getFill(strategyName, ExchangeManager.BTSE_EXCHANGE_NAME);
		// 取得當前orderbook的方法
		Orderbook orderbook = exchangeManager.getOrderbook(ExchangeManager.BTSE_EXCHANGE_NAME, "ETHPFC");
		// 還拿不到orderbook 先return
		if(orderbook == null) return;
		
		// 當目標價大於0表示有進場  且市場價格已大於等於目標價 停利
		if(targetPrice > 0 && orderbook.getBidTopPrice(1)[0] >= targetPrice) {
			stopProfit(orderbook.getBidTopPrice(1)[0]);
		}
		Double price = orderbook.getAskTopPrice(1)[0];
		if(position == 0) {
			// 無條件市價進場 市價單價格為0
//			String orderId = exchangeManager.sendOrder(strategyName, ExchangeManager.BTSE_EXCHANGE_NAME, userName, "ETHPFC", BuySell.BUY, 0, firstContractSize);
			// 假設已經進場 更新倉位
			log.info("open order: {}, {} contracts", price, firstContractSize);
			position = firstContractSize;
			averagePrice = price;
			targetPrice = averagePrice * stopProfit;
			currentFees += price * firstContractSize * fee;
			currentLevel = 1;
			for(int i = 0 ; i < orderLevel - 1; i++) {
				double lastPrice = i == 0 ? price : priceLevel.get(i - 1);
				priceLevel.add(lastPrice - firstContractSize * Math.pow(2, i));
			}
			// targetPrice 可以先下停利單
		} else if(currentLevel > 0) {
			if(price <= priceLevel.get(currentLevel -1)) {
				log.info("open order: {}, {} contracts", price, position);
				averagePrice = (averagePrice * position) + (price * position) / (position * 2);
				position += position;
				targetPrice = averagePrice * stopProfit;
				currentFees += price * firstContractSize * fee;
				currentLevel ++;
			}
		}
	}
	
	private void stopProfit(double price) {
		double pnl = (price - averagePrice) * (position / 100) - currentFees;
		
		log.info("stop profit: {}, {} contracts, pnl:{}", price, position, pnl);
		position = 0;
		averagePrice = 0;
		targetPrice = 0;
		currentFees = 0;
		priceLevel.clear();
	}
	
	public static void main(String[] s) {
		Properties prop = System.getProperties();
		prop.setProperty("log4j.configurationFile", "./config/log4j2.xml");
		
		try {
			StrategyConfig strategyConfig = new JsonStrategyConfig("./config/test.json");
			Strategy strategy = new GridTestStrategy(strategyConfig);
			strategy.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
