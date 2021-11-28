package name.qd.tp2.strategies.config;

import java.util.Set;

import name.qd.tp2.exchanges.vo.ApiKeySecret;

public interface StrategyConfig {
	public Set<String> getAllExchange();
	public String getExchangeEvn(String exchange);
	public String getExchangeFillChannel(String exchange);
	public Set<String> getAllSymbols(String exchange);
	public void addSymbol(String exchange, String symbol);
	// trailing order 類型 目前沒用
	// TODO 預期支援 FIX & RATE
	public String getTrailingType();
	// trailing order 回彈多少下單
	public double getTrailingValue();
	// trailing order 觸發價格正負多少無視
	public double getPullbackTolerance();
	public String getCustomizeSettings(String key);
	
	public Set<String> getAllUser(String exchange);
	public ApiKeySecret getApiKeySecret(String exchange, String user);
}
