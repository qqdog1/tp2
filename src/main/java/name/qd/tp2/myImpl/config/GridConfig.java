package name.qd.tp2.myImpl.config;

import name.qd.tp2.exchanges.ExchangeManager;
import name.qd.tp2.strategies.config.CodeStrategyConfig;

public class GridConfig extends CodeStrategyConfig {
	public GridConfig() {
		addSymbol(ExchangeManager.BTSE_EXCHANGE_NAME, "ETHPFC");
	}
}
