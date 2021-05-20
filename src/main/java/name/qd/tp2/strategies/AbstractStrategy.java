package name.qd.tp2.strategies;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.qd.tp2.exchanges.ExchangeManager;
import name.qd.tp2.strategies.config.StrategyConfig;

public abstract class AbstractStrategy implements Strategy {
	private Logger log = LoggerFactory.getLogger(AbstractStrategy.class); 
	protected ExchangeManager exchangeManager = ExchangeManager.getInstance();
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	
	private StrategyConfig strategyConfig;
	
	public AbstractStrategy(StrategyConfig strategyConfig) {
		this.strategyConfig = strategyConfig;
		
		initAllExchange();
		
		while(true) {
			if(isAllExchangeReady()) {
				break;
			}
			
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		subscribeAllSymbol();
		setUserInfo();
	}
	
	public void start() {
		executor.execute(new StrategyCycle());
	}
	
	public abstract void strategyAction();
	
	private void initAllExchange() {
		Set<String> exchanges = strategyConfig.getAllExchange();
		for(String exchange : exchanges) {
			exchangeManager.initExchange(exchange);
		}
	}
	
	private void setUserInfo() {
		Set<String> exchanges = strategyConfig.getAllExchange();
		for(String exchange : exchanges) {
			Set<String> users = strategyConfig.getAllUser(exchange);
			for(String userName : users) {
				exchangeManager.addUserApiKeySecret(exchange, userName, strategyConfig.getApiKeySecret(exchange, userName));
			}
		}
	}
	
	private boolean isAllExchangeReady() {
		Set<String> exchanges = strategyConfig.getAllExchange();
		int readyCount = 0;
		for(String exchange : exchanges) {
			if(exchangeManager.isExchangeReady(exchange)) {
				readyCount++;
			} else {
				log.error("{} exchange not ready.", exchange);
			}
		}
		return exchanges.size() == readyCount;
	}
	
	private void subscribeAllSymbol() {
		Set<String> exchanges = strategyConfig.getAllExchange();
		for(String exchange : exchanges) {
			Set<String> symbols = strategyConfig.getAllSymbols(exchange);
			for(String symbol : symbols) {
				exchangeManager.subscribe(exchange, symbol);
			}
		}
	}
	
	private class StrategyCycle implements Runnable {
		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				strategyAction();
			}
		}
	}
}
