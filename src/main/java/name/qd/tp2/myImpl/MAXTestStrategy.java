package name.qd.tp2.myImpl;

import java.util.List;
import java.util.Properties;

import name.qd.tp2.exchanges.ExchangeManager;
import name.qd.tp2.exchanges.vo.MarketInfo;
import name.qd.tp2.strategies.AbstractStrategy;
import name.qd.tp2.strategies.config.CodeStrategyConfig;
import name.qd.tp2.strategies.config.JsonStrategyConfig;
import name.qd.tp2.strategies.config.StrategyConfig;

public class MAXTestStrategy extends AbstractStrategy {
	
	private String tradingExchange;
	private List<MarketInfo> lstMarketInfo;

	public MAXTestStrategy(String strategyName, StrategyConfig strategyConfig) {
		super(strategyName, strategyConfig);
		
		tradingExchange = strategyConfig.getCustomizeSettings("tradingExchange");
		
		lstMarketInfo = exchangeManager.getMarkets(tradingExchange);
	}

	@Override
	public void strategyAction() {
		
	}
	
	public static void main(String[] s) {
		Properties prop = System.getProperties();
		prop.setProperty("log4j.configurationFile", "./config/log4j2.xml");

		try {
			String configPath = "./config/max.json";
//			MAXTestStrategy strategy = new MAXTestStrategy("", new CodeStrategyConfig(ExchangeManager.MAX_EXCHANGE, "btctwd", "production"));
			MAXTestStrategy strategy = new MAXTestStrategy("MAX price monitor", new JsonStrategyConfig(configPath));
			strategy.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
