package name.qd.tp2.exchanges;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExchangeSettings {
	private static Map<String, Map<String, List<String>>> map = new HashMap<>();
	
	static {
		map.put(ExchangeManager.BTSE_EXCHANGE, new HashMap<>());
		map.get(ExchangeManager.BTSE_EXCHANGE).put("production", new ArrayList<>());
		map.get(ExchangeManager.BTSE_EXCHANGE).put("testnet", new ArrayList<>());
		map.get(ExchangeManager.BTSE_EXCHANGE).get("production").add("https://api.btse.com/futures");
		map.get(ExchangeManager.BTSE_EXCHANGE).get("production").add("wss://ws.btse.com/ws/futures");
		map.get(ExchangeManager.BTSE_EXCHANGE).get("testnet").add("https://testapi.btse.io/futures");
		map.get(ExchangeManager.BTSE_EXCHANGE).get("testnet").add("wss://testws.btse.io/ws/futures");
		
		map.put(ExchangeManager.MAX_EXCHANGE, new HashMap<>());
		map.get(ExchangeManager.MAX_EXCHANGE).put("production", new ArrayList<>());
		map.get(ExchangeManager.MAX_EXCHANGE).get("production").add("https://max-api.maicoin.com/api/v2");
		map.get(ExchangeManager.MAX_EXCHANGE).get("production").add("wss://max-stream.maicoin.com/ws");
	};
	
	public static String getExchangeRestUrlByEnv(String exchange, String env) {
		return map.get(exchange).get(env).get(0);
	}
	
	public static String getExchangeWSUrlByEnv(String exchange, String env) {
		return map.get(exchange).get(env).get(1);
	}
}
