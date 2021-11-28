package name.qd.tp2.strategies.config;

import java.io.File;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import name.qd.tp2.utils.JsonUtils;

public class JsonStrategyConfig extends AbstractStrategyConfig {
	private Logger log = LoggerFactory.getLogger(JsonStrategyConfig.class);
	
	public JsonStrategyConfig(String filePath) throws Exception {
		File file = new File(filePath);
		JsonNode node = JsonUtils.objectMapper.readTree(file);
		
		JsonNode generalSettings = node.get("generalSettings");
		
		// TODO json 格式欄位檢查補完
		if(!generalSettings.isArray()) {
			log.error("Json config最外層必須是array");
			throw new Exception("Json config最外層必須是array");
		}
		
		for(JsonNode exchangeNode : generalSettings) {
			String exchange = exchangeNode.get("exchange").asText();
			if(exchangeNode.has("env")) {
				String env = exchangeNode.get("env").asText();
				setExchangeEvn(exchange, env);
			}
			if(exchangeNode.has("fillChannel")) {
				String fillChannel = exchangeNode.get("fillChannel").asText();
				setExchangeFillChannel(exchange, fillChannel);
			}
			if(exchangeNode.has("trailingType")) {
				String trailingType = exchangeNode.get("trailingType").asText();
				setTrailingType(trailingType);
				
				double trailingValue = exchangeNode.get("trailingValue").asDouble();
				setTrailingValue(trailingValue);
				
				double pullbackTolerance = exchangeNode.get("pullbackTolerance").asDouble();
				setPullbackTolerance(pullbackTolerance);
			}
			
			for(JsonNode symbolNode : exchangeNode.get("symbol")) {
				addSymbol(exchange, symbolNode.asText());
			}
			
			JsonNode userNode = exchangeNode.get("user");
			for(JsonNode user : userNode) {
				String name = user.get("name").asText();
				String apiKey = user.get("apiKey").asText();
				String secret = user.get("secret").asText();
				addApiKeySecret(exchange, name, apiKey, secret);
			}
		}
		
		// 
		JsonNode customizeSettings = node.get("customizeSettings");
		
		Iterator<String> customizeNode = customizeSettings.fieldNames();
		while(customizeNode.hasNext()) {
			String key = customizeNode.next();
			addCustomizeSettings(key, customizeSettings.get(key).asText());
		}
	}
}
