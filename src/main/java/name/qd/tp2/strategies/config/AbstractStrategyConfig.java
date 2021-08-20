package name.qd.tp2.strategies.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import name.qd.tp2.exchanges.vo.ApiKeySecret;
import name.qd.tp2.strategies.vo.TradingPair;

public abstract class AbstractStrategyConfig implements StrategyConfig {
	private TradingPair tradingPair = new TradingPair();
	private Map<String, Map<String, ApiKeySecret>> mapApiKeys = new HashMap<>();
	private Map<String, String> mapCustomize = new HashMap<>();
	private Map<String, String> mapExchangeEnv = new HashMap<>();
	
	public Set<String> getAllExchange() {
		return tradingPair.getAllExchange();
	}
	
	public String getExchangeEvn(String exchange) {
		return mapExchangeEnv.get(exchange);
	}
	
	public void setExchangeEvn(String exchange, String env) {
		mapExchangeEnv.put(exchange, env);
	}
	
	public Set<String> getAllSymbols(String exchange) {
		return tradingPair.getAllSymbol(exchange);
	}
	
	public void addSymbol(String exchange, String symbol) {
		tradingPair.addSymbol(exchange, symbol);
	}
	
	public void addApiKeySecret(String exchange, String userName, String apiKey, String secret) {
		if(!mapApiKeys.containsKey(exchange)) {
			mapApiKeys.put(exchange, new HashMap<>());
		}
		ApiKeySecret apiKeySecret = new ApiKeySecret(apiKey, secret);
		mapApiKeys.get(exchange).put(userName, apiKeySecret);
	}
	
	public Set<String> getAllUser(String exchange) {
		if(mapApiKeys.containsKey(exchange)) {
			return mapApiKeys.get(exchange).keySet();
		}
		return Collections.emptySet();
	}
	
	public ApiKeySecret getApiKeySecret(String exchange, String userName) {
		return mapApiKeys.get(exchange).get(userName);
	}
	
	public void addCustomizeSettings(String key, String value) {
		mapCustomize.put(key, value);
	}
	
	public String getCustomizeSettings(String key) {
		return mapCustomize.get(key);
	}
}
