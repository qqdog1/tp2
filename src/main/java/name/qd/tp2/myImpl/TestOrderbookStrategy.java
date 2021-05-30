package name.qd.tp2.myImpl;

import name.qd.tp2.constants.BuySell;
import name.qd.tp2.exchanges.ExchangeManager;
import name.qd.tp2.exchanges.vo.Orderbook;
import name.qd.tp2.strategies.AbstractStrategy;
import name.qd.tp2.strategies.config.JsonStrategyConfig;
import name.qd.tp2.strategies.config.StrategyConfig;

public class TestOrderbookStrategy extends AbstractStrategy {

	public TestOrderbookStrategy(StrategyConfig strategyConfig) {
		super(strategyConfig);
	}
	
	@Override
	public void strategyAction() {
		processBook();
		
//		exchangeManager.getBalance(ExchangeManager.BTSE_EXCHANGE_NAME, "shawn");
		String orderId = exchangeManager.sendOrder("Test01", ExchangeManager.BTSE_EXCHANGE_NAME, "shawn", 
				"ETHPFC", BuySell.BUY, 1, 1);
		
		System.out.println(orderId);
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		boolean isCancelled = exchangeManager.cancelOrder("Test01", ExchangeManager.BTSE_EXCHANGE_NAME, "shawn", "ETHPFC", orderId);
		System.out.println(isCancelled);
	}

	private void processBook() {
		Orderbook orderbook = exchangeManager.getOrderbook(ExchangeManager.BTSE_EXCHANGE_NAME, "ETHPFC");
		if(orderbook != null) {
			Double bidPrice = orderbook.getBidTopPrice(1)[0];
			Double bidQty = orderbook.getBidTopQty(1)[0];
			Double askPrice = orderbook.getAskTopPrice(1)[0];
			Double askQty = orderbook.getAskTopQty(1)[0];
			
			System.out.println(bidPrice + ":" + bidQty + ", " + askPrice + ":" + askQty);
		}
	}
	
	public static void main(String[] s) {
		try {
			StrategyConfig strategyConfig = new JsonStrategyConfig("./config/test.json");
			TestOrderbookStrategy strategy = new TestOrderbookStrategy(strategyConfig);
			strategy.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
