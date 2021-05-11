package name.qd.tp2.exchanges.vo;

public class ApiKeySecret {
	private String apiKey;
	private String secret;
	public ApiKeySecret(String apiKey, String secret) {
		this.apiKey = apiKey;
		this.secret = secret;
	}
	public String getApiKey() {
		return apiKey;
	}
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}
	public String getSecret() {
		return secret;
	}
	public void setSecret(String secret) {
		this.secret = secret;
	}
}
