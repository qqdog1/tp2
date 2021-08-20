package name.qd.tp2.exchanges;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExchangeSettings {
	private static Map<String, Map<String, List<String>>> map = new HashMap<>();
	
	static {
		map.put("BTSE", new HashMap<>());
		map.get("BTSE").put("production", new ArrayList<>());
		map.get("BTSE").put("testnet", new ArrayList<>());
		map.get("BTSE").get("production").add("https://api.btse.com/futures/");
		map.get("BTSE").get("production").add("wss://ws.btse.com/ws/futures");
		map.get("BTSE").get("testnet").add("https://testapi.btse.io/futures");
		map.get("BTSE").get("testnet").add("wss://testws.btse.io/ws/futures");
	};
	
	public static String getExchangeRestUrlByEnv(String exchange, String env) {
		return map.get(exchange).get(env).get(0);
	}
	
	public static String getExchangeWSUrlByEnv(String exchange, String env) {
		return map.get(exchange).get(env).get(1);
	}
}
