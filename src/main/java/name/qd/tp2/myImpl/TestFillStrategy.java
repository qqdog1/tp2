package name.qd.tp2.myImpl;

import java.util.Properties;

import name.qd.tp2.exchanges.ExchangeManager;
import name.qd.tp2.strategies.AbstractStrategy;
import name.qd.tp2.strategies.Strategy;
import name.qd.tp2.strategies.config.JsonStrategyConfig;
import name.qd.tp2.strategies.config.StrategyConfig;

public class TestFillStrategy extends AbstractStrategy {
	private ExchangeManager exchangeManager = ExchangeManager.getInstance();

	public TestFillStrategy(StrategyConfig strategyConfig) {
		super(strategyConfig);
	}

	@Override
	public void strategyAction() {
	}

	public static void main(String[] s) {
		Properties prop = System.getProperties();
		prop.setProperty("log4j.configurationFile", "./config/log4j2.xml");
		
		try {
			StrategyConfig strategyConfig = new JsonStrategyConfig("./config/test.json");
			Strategy strategy = new TestFillStrategy(strategyConfig);
			strategy.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
