package name.qd.tp2.myImpl;

import java.util.Properties;

import name.qd.tp2.exchanges.ExchangeManager;
import name.qd.tp2.strategies.AbstractStrategy;
import name.qd.tp2.strategies.config.CodeStrategyConfig;
import name.qd.tp2.strategies.config.StrategyConfig;

public class MAXTestStrategy extends AbstractStrategy {

	public MAXTestStrategy(String strategyName, StrategyConfig strategyConfig) {
		super(strategyName, strategyConfig);
	}

	@Override
	public void strategyAction() {
		
	}
	
	public static void main(String[] s) {
		Properties prop = System.getProperties();
		prop.setProperty("log4j.configurationFile", "./config/log4j2.xml");

		try {
			MAXTestStrategy strategy = new MAXTestStrategy("", new CodeStrategyConfig(ExchangeManager.MAX_EXCHANGE, "btctwd", "production"));
			strategy.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
}
