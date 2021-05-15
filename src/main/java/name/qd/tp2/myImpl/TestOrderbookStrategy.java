package name.qd.tp2.myImpl;

import name.qd.tp2.exchanges.ExchangeManager;
import name.qd.tp2.exchanges.vo.Orderbook;
import name.qd.tp2.myImpl.config.TestOrderbookConfig;
import name.qd.tp2.strategies.AbstractStrategy;
import name.qd.tp2.strategies.config.StrategyConfig;

public class TestOrderbookStrategy extends AbstractStrategy {

	public TestOrderbookStrategy(StrategyConfig strategyConfig) {
		super(strategyConfig);
	}

	@Override
	public void processBook() {
		Orderbook orderbook = exchangeManager.getOrderbook(ExchangeManager.BTSE_EXCHANGE_NAME, "ETHPFC");
		if(orderbook != null) {
			Double bidPrice = orderbook.getBidTopPrice(1)[0];
			Double bidQty = orderbook.getBidTopQty(1)[0];
			Double askPrice = orderbook.getAskTopPrice(1)[0];
			Double askQty = orderbook.getAskTopQty(1)[0];
			
//			System.out.println(bidPrice + ":" + bidQty + ", " + askPrice + ":" + askQty);
		}
	}

	@Override
	public void processFill() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void strategyAction() {
		// TODO Auto-generated method stub
		
	}
	
	public static void main(String[] s) {
		TestOrderbookConfig config = new TestOrderbookConfig();
		TestOrderbookStrategy strategy = new TestOrderbookStrategy(config);
		
		strategy.start();
	}
}
