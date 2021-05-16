package name.qd.tp2.strategies.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import name.qd.tp2.exchanges.vo.ApiKeySecret;
import name.qd.tp2.strategies.vo.TradingPair;

public abstract class AbstractStrategyConfig implements StrategyConfig {
	private Map<String, String> mapConfigs = new HashMap<>();
	private TradingPair tradingPair = new TradingPair();
	private Map<String, Map<String, ApiKeySecret>> mapApiKeys = new HashMap<>();
	private Map<String, String> mapCustomize = new HashMap<>();
	
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
	
	public void addApiKeySecret(String exchange, String user, String apiKey, String secret) {
		if(!mapApiKeys.containsKey(exchange)) {
			mapApiKeys.put(exchange, new HashMap<>());
		}
		ApiKeySecret apiKeySecret = new ApiKeySecret(apiKey, secret);
		mapApiKeys.get(exchange).put(user, apiKeySecret);
	}
	
	public Set<String> getAllUser(String exchange) {
		if(mapApiKeys.containsKey(exchange)) {
			return mapApiKeys.get(exchange).keySet();
		}
		return Collections.emptySet();
	}
	
	public ApiKeySecret getApiKeySecret(String exchange, String user) {
		return mapApiKeys.get(exchange).get(user);
	}
	
	public void addCustomizeSettings(String key, String value) {
		mapCustomize.put(key, value);
	}
	
	public String getCustomizeSettings(String key) {
		return mapCustomize.get(key);
	}
}
