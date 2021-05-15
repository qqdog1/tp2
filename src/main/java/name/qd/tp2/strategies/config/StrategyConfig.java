package name.qd.tp2.strategies.config;

import java.util.Set;

public interface StrategyConfig {
	public Set<String> getAllExchange();
	public Set<String> getAllSymbols(String exchange);
	public void addSymbol(String exchange, String symbol);
	public String getConfig(String key);
}
