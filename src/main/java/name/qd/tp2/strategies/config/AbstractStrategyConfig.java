package name.qd.tp2.strategies.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import name.qd.tp2.exchanges.vo.ApiKeySecret;

public abstract class AbstractStrategyConfig implements StrategyConfig {
	private Map<String, Set<String>> mapTradingPair = new HashMap<>();
	private Map<String, Map<String, ApiKeySecret>> mapApiKeys = new HashMap<>();
	private Map<String, String> mapCustomize = new HashMap<>();
	private Map<String, String> mapExchangeEnv = new HashMap<>();
	private Map<String, String> mapExchangeFillChannel = new HashMap<>();
	private String trailingType;
	private double trailingValue;
	private double pullbackTolerance;
	
	public void addExchange(String exchange) {
		if(!mapTradingPair.containsKey(exchange)) {
			mapTradingPair.put(exchange, new HashSet<>());
		}
	}
	
	public void addSymbol(String exchange, String symbol) {
		addExchange(exchange);
		mapTradingPair.get(exchange).add(symbol);
	}
	
	public Set<String> getAllExchange() {
		return mapTradingPair.keySet();
	}
	
	public Set<String> getAllSymbols(String exchange) {
		return mapTradingPair.get(exchange);
	}
	
	public String getExchangeEvn(String exchange) {
		return mapExchangeEnv.get(exchange);
	}
	
	public void setExchangeEvn(String exchange, String env) {
		mapExchangeEnv.put(exchange, env);
	}
	
	public String getExchangeFillChannel(String exchange) {
		return mapExchangeFillChannel.get(exchange);
	}
	
	public void setExchangeFillChannel(String exchange, String fillChannel) {
		mapExchangeFillChannel.put(exchange, fillChannel);
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
	
	public String getTrailingType() {
		return trailingType;
	}
	
	public void setTrailingType(String trailingType) {
		this.trailingType = trailingType;
	}
	
	public double getTrailingValue() {
		return trailingValue;
	}
	
	public void setTrailingValue(double trailingValue) {
		this.trailingValue = trailingValue;
	}
	
	public double getPullbackTolerance() {
		return pullbackTolerance;
	}
	
	public void setPullbackTolerance(double pullbackTolerance) {
		this.pullbackTolerance = pullbackTolerance;
	}
	
	public void addCustomizeSettings(String key, String value) {
		mapCustomize.put(key, value);
	}
	
	public String getCustomizeSettings(String key) {
		return mapCustomize.get(key);
	}
}
