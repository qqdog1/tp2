package name.qd.tp2.strategies.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import name.qd.tp2.strategies.vo.TradingPair;

public abstract class CodeStrategyConfig implements StrategyConfig {
	private Map<String, String> mapConfigs = new HashMap<>();
	private TradingPair tradingPair = new TradingPair();
	
	public Set<String> getAllExchange() {
		return tradingPair.getAllExchange();
	}
	
	public Set<String> getAllSymbols(String exchange) {
		return tradingPair.getAllSymbol(exchange);
	}
	
	public void addSymbol(String exchange, String symbol) {
		tradingPair.addSymbol(exchange, symbol);
	}
	
	public void setConfig(String key, String value) {
		mapConfigs.put(key, value);
	}
	
	public String getConfig(String key) {
		return mapConfigs.get(key);
	}
}
