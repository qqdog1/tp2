package name.qd.tp2.myImpl;

import java.util.List;
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

public class GridStrategy extends AbstractStrategy {
	private Logger log = LoggerFactory.getLogger(GridStrategy.class);
	private LineNotifyUtils lineNotifyUtils;
	// 與交易所溝通用
	private ExchangeManager exchangeManager = ExchangeManager.getInstance();
	
	private String strategyName = "Grid1";
	private String userName = "shawn";

	// 自己設定一些策略內要用的變數
	private String symbol = "ETHPFC";
	private String firstOrderId;
	private String stopProfitOrderId;
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
	private String lineNotify;
	
	public GridStrategy(StrategyConfig strategyConfig) {
		super(strategyConfig);

		// 把設定值從config讀出來
		priceRange = Integer.parseInt(strategyConfig.getCustomizeSettings("priceRange"));
		firstContractSize = Integer.parseInt(strategyConfig.getCustomizeSettings("firstContractSize"));
		stopProfit = Double.parseDouble(strategyConfig.getCustomizeSettings("stopProfit"));
		fee = Double.parseDouble(strategyConfig.getCustomizeSettings("fee"));
		tickSize = Double.parseDouble(strategyConfig.getCustomizeSettings("tickSize"));
		orderLevel = Integer.parseInt(strategyConfig.getCustomizeSettings("orderLevel"));
		lineNotify = strategyConfig.getCustomizeSettings("lineNotify");
		
		lineNotifyUtils = new LineNotifyUtils(lineNotify);
	}

	@Override
	public void strategyAction() {
		checkFill();
		if(position == 0) {
			setFirstOrder();
		} else {
			checkFirstOrderProfit();
		}
		
		// 給GTC多一點時間 且電腦要爆了
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void setFirstOrder() {
		if(firstOrderId != null) {
			// cancel order
			if(!cancelOrder(firstOrderId)) {
				log.error("刪除第一筆單失敗 orderId:{}", firstOrderId);
			} else {
				log.debug("刪除未成交第一單 {}", firstOrderId);
				firstOrderId = null;
			}
		}
		
		// 上面刪單成功 id變null, 或是本來就沒單
		// 或是第一單已經成交
		if(firstOrderId == null && stopProfitOrderId == null) {
			Orderbook orderbook = exchangeManager.getOrderbook(ExchangeManager.BTSE_EXCHANGE_NAME, symbol);
			if(orderbook == null) return;
			double price = orderbook.getBidTopPrice(1)[0] + tickSize;
			// 可能要用賣價減一個ticksize
			String orderId = sendOrder(BuySell.BUY, price, firstContractSize);
			if(orderId != null) {
				log.info("送出第一單 {},{} , {}", price, firstContractSize, orderId);
				firstOrderId = orderId;
			} else {
				log.error("送出第一單失敗");
			}
		}
	}
	
	private void checkFirstOrderProfit() {
		// 第一單存在的狀況下
		// 如果第一單達目標價
		// 將第一單算成新的一次交易的第一單
		// 重新鋪單
		if(position == firstContractSize) {
			// 第一單完全成交狀態下
			Orderbook orderbook = exchangeManager.getOrderbook(ExchangeManager.BTSE_EXCHANGE_NAME, symbol);
			if(orderbook != null) {
				double price = orderbook.getBidTopPrice(1)[0];
				if(price >= targetPrice) {
					log.info("第一單到達停利價格 直接當下一單的第一單");
					// 刪除所有鋪單
					cancelOrder(null);
					// 更新成本價格
					averagePrice = targetPrice;
					// 更新停利價格
					targetPrice = averagePrice * stopProfit;
					targetPrice -= targetPrice % tickSize;
					// 重新鋪單
					placeLevelOrders(averagePrice);
				}
			}
		}
	}

	private void checkFill() {
		List<Fill> lstFill = exchangeManager.getFill(strategyName, ExchangeManager.BTSE_EXCHANGE_NAME);
		for(Fill fill : lstFill) {
			log.debug("收到成交 {} {} {}", fill.getPrice(), fill.getQty(), fill.getOrderId());
			if(position < firstContractSize) {
				averagePrice = ((position * averagePrice) + (fill.getPrice() * fill.getQty())) / (position + fill.getQty());
				position += fill.getQty();
				if(position == firstContractSize) {
					log.info("第一單完全成交");
					// 更新目標價
					targetPrice = averagePrice * stopProfit;
					targetPrice -= targetPrice % tickSize;
					// 完全成交
					// 鋪單
					placeLevelOrders(averagePrice);
				} else {
					log.warn("第一單部分成交 {} {}", fill.getPrice(), fill.getQty());
				}
			} else {
				if(fill.getOrderId().equals(stopProfitOrderId)) {
					// 停利單成交
					position -= fill.getQty();
					if(position == firstContractSize ) {
						log.info("停利單完全成交");
						// 刪除之前鋪單
						cancelOrder(null);
						// 鋪單
						placeLevelOrders(fill.getPrice());
					} else {
						log.warn("停利單部分成交 {}, {}", fill.getPrice(), fill.getQty());
					}
				} else {
					// 一般單成交
					
					// 重算成本  改停利單
					averagePrice = ((position * averagePrice) + (fill.getPrice() * fill.getQty())) / (position + fill.getQty());
					position += fill.getQty();
					// 清除舊的停利單
					if(stopProfitOrderId != null) {
						if(!cancelOrder(stopProfitOrderId)) {
							log.error("清除舊的停利單失敗 orderId:{}", stopProfitOrderId);
						}
					}
					// 下新的停利單
					targetPrice = averagePrice * stopProfit;
					targetPrice -= targetPrice % tickSize;
					String orderId = sendOrder(BuySell.SELL, targetPrice, position - firstContractSize);
					if(orderId != null) {
						stopProfitOrderId = orderId;
						log.info("下停利單 {} {} {}", targetPrice, position - firstContractSize, orderId);
					} else {
						log.warn("下停利單失敗");
					}
				}
			}
		}
	}
	
	private void placeLevelOrders(double basePrice) {
		for(int i = 1 ; i < orderLevel ; i++) {
			basePrice = basePrice - (priceRange * Math.pow(2, i-1));
			double qty = firstContractSize * Math.pow(2, i-1);
			String orderId = sendOrder(BuySell.BUY, basePrice, qty);
			if(orderId != null) {
				log.info("鋪單 {} {} {}", i, basePrice, qty);
			} else {
				log.error("鋪單失敗 {} {} {}", i, basePrice, qty);
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
			GridStrategy strategy = new GridStrategy(new JsonStrategyConfig("./config/testnet.json"));
			strategy.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
