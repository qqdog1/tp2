package name.qd.tp2.myImpl;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import name.qd.tp2.constants.BuySell;
import name.qd.tp2.exchanges.ExchangeManager;
import name.qd.tp2.exchanges.vo.Fill;
import name.qd.tp2.strategies.AbstractStrategy;
import name.qd.tp2.strategies.Strategy;
import name.qd.tp2.strategies.config.JsonStrategyConfig;
import name.qd.tp2.strategies.config.StrategyConfig;

public class TestFillStrategy extends AbstractStrategy {
	private Logger log = LoggerFactory.getLogger(TestFillStrategy.class);
	private ExchangeManager exchangeManager = ExchangeManager.getInstance();
	private String symbol = "ETHPFC";
	private String username = "shawn";
	
	private int position = 0;
	private double cost = 0;
	private double averagePrice = 0;
	private double fee;
	
	private int totalFill = 0;
	
	private List<BigDecimal> lstBuy = new ArrayList<>();
	private List<BigDecimal> lstSell = new ArrayList<>();
	private ObjectMapper objectMapper = new ObjectMapper();

	public TestFillStrategy(StrategyConfig strategyConfig) {
		super(strategyConfig);
		
		fee = Double.parseDouble(strategyConfig.getCustomizeSettings("fee"));
	}

	@Override
	public void strategyAction() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = null;
		try {
			sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
			date = sdf.parse("2021-08-23 18:19:00");
//			date = sdf.parse("2021-08-27 00:00:00");
		} catch (ParseException e) {
			e.printStackTrace();
		}
		long from = date.getTime();
		ZonedDateTime zonedDateTime = ZonedDateTime.now();
		long to = zonedDateTime.toEpochSecond() * 1000;
		
		boolean isLast = false;
		while(!isLast) {
			List<Fill> lst = exchangeManager.getFillHistory(ExchangeManager.BTSE_EXCHANGE_NAME, username, symbol, from, to);
			log.info("get fill size: {}", lst.size());
			totalFill += lst.size();
			for(Fill fill : lst) {
				if(Integer.parseInt(fill.getQty()) == 1) {
					try {
						log.info(objectMapper.writeValueAsString(fill));
					} catch (JsonProcessingException e) {
						e.printStackTrace();
					}
					
					calcAvgPrice(fill);
					
					BigDecimal price = new BigDecimal(fill.getOrderPrice());
					if(fill.getBuySell() == BuySell.BUY) {
						if(lstSell.contains(price.add(BigDecimal.valueOf(6)))) {
							if(!lstSell.remove(price.add(BigDecimal.valueOf(6)))) {
								log.error("刪除cache失敗");
							}
						} else {
							lstBuy.add(price);
						}
					} else if(fill.getBuySell() == BuySell.SELL) {
						if(lstBuy.contains(price.subtract(BigDecimal.valueOf(6)))) {
							if(!lstBuy.remove(price.subtract(BigDecimal.valueOf(6)))) {
								log.error("刪除cache失敗");
							}
						} else {
							lstSell.add(price);
						}
					} else {
						log.error("unknown side");
					}
				}
				to = fill.getTimestamp();
			}
			
			if(lst.size() < 100) {
				isLast = true;
			}
		}
		
		for(BigDecimal price : lstBuy) {
			log.info("Buy: {}", price);
		}
		for(BigDecimal price : lstSell) {
			log.info("Sell: {}", price);
		}
		
		log.info("total: {}", totalFill);
		
		System.exit(0);
	}
	
	private void calcAvgPrice(Fill fill) {
		int qty = Integer.parseInt(fill.getQty());
		double fillPrice = Double.parseDouble(fill.getFillPrice());
		if(BuySell.BUY == fill.getBuySell()) {
			position += Integer.parseInt(fill.getQty());
			double feeCost = qty * fillPrice * fee;
			cost = cost + (qty * fillPrice) + feeCost;
			averagePrice = cost / position;
		} else {
			position -= qty;
			double feeCost = qty * fillPrice * fee;
			cost = cost - (qty * fillPrice) + feeCost;
			averagePrice = cost / position;
		}
		log.info("position: {}, cost: {}, avgPrice: {}", position, cost, averagePrice);
	}

	public static void main(String[] s) {
		Properties prop = System.getProperties();
		prop.setProperty("log4j.configurationFile", "./config/log4j2.xml");
		
		try {
			StrategyConfig strategyConfig = new JsonStrategyConfig("./config/test.json");
			Strategy strategy = new TestFillStrategy(strategyConfig);
			strategy.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
