package name.qd.tp2.strategies.config;

public class CodeStrategyConfig extends AbstractStrategyConfig {
	
	public CodeStrategyConfig(String exchange, String symbol, String env) {
		addSymbol(exchange, symbol);
		setExchangeEvn(exchange, env);
	}
}
