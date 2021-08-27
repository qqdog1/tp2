package name.qd.tp2.myImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.qd.tp2.exchanges.ExchangeManager;
import name.qd.tp2.exchanges.vo.Fill;
import name.qd.tp2.strategies.AbstractStrategy;
import name.qd.tp2.strategies.config.JsonStrategyConfig;
import name.qd.tp2.strategies.config.StrategyConfig;

public class FillRecordCSV extends AbstractStrategy {
	private Logger log = LoggerFactory.getLogger(FillRecordCSV.class);
	
	private ExchangeManager exchangeManager = ExchangeManager.getInstance();
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private String startTime;
	
	public FillRecordCSV(StrategyConfig strategyConfig) {
		super(strategyConfig);
		
		startTime = strategyConfig.getCustomizeSettings("startTime");
		
	}

	@Override
	public void strategyAction() {
		Date date = null;
		try {
			sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
			date = sdf.parse(startTime);
		} catch (ParseException e) {
		}
		
		long from = date.getTime();
		ZonedDateTime zonedDateTime = ZonedDateTime.now();
		long to = zonedDateTime.toEpochSecond() * 1000;
		
		
		try {
			Files.write(Paths.get("C:\\Users\\shawn\\Desktop\\fill.csv"), "orderId,buySell,price,qty,time".getBytes(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
			Files.write(Paths.get("C:\\Users\\shawn\\Desktop\\fill.csv"), System.lineSeparator().getBytes(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
		} catch (IOException e) {
			e.printStackTrace();
		}   
		
		boolean isLast = false;
		while(!isLast) {
			List<Fill> lst = exchangeManager.getFillHistory(ExchangeManager.BTSE_EXCHANGE_NAME, "shawn", "ETHPFC", from, to);
			
			
			for(Fill fill : lst) {
				if(Integer.parseInt(fill.getQty()) != 1) continue;
				
				StringBuilder sb = new StringBuilder();
				sb.append(fill.getOrderId()).append(",")
				.append(fill.getBuySell()).append(",")
				.append(fill.getFillPrice()).append(",")
				.append(fill.getQty()).append(",")
				.append(fill.getTimestamp());
				
				try {
					Files.write(Paths.get("C:\\Users\\shawn\\Desktop\\fill.csv"), sb.toString().getBytes(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
					Files.write(Paths.get("C:\\Users\\shawn\\Desktop\\fill.csv"), System.lineSeparator().getBytes(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
				} catch (IOException e) {
					e.printStackTrace();
				} 
				
				to = fill.getTimestamp();
			}
			
			if(lst.size() < 100) {
				isLast = true;
			}
		}
		
		System.exit(0);
	}
	
	public static void main(String[] s) {
		Properties prop = System.getProperties();
		prop.setProperty("log4j.configurationFile", "./config/log4j2.xml");

		try {
			String configPath = "./config/grid2.json";
			FillRecordCSV strategy = new FillRecordCSV(new JsonStrategyConfig(configPath));
			strategy.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
