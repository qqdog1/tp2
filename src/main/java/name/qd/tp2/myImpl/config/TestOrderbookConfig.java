package name.qd.tp2.myImpl.config;

import name.qd.tp2.exchanges.ExchangeManager;
import name.qd.tp2.strategies.config.CodeStrategyConfig;

public class TestOrderbookConfig extends CodeStrategyConfig {
	public TestOrderbookConfig() {
		addSymbol(ExchangeManager.BTSE_EXCHANGE_NAME, "ETHPFC");
	}
}
