package name.qd.tp2.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.jupiter.api.Test;

import name.qd.tp2.constants.StopProfitType;

public class PriceUtilsTest {

	@Test
	public void trimPriceWithTicksizeTest() {
		BigDecimal price = new BigDecimal("10.1");
		BigDecimal tickSize = new BigDecimal("0.1");
		assertEquals(new BigDecimal("10.1").doubleValue(), PriceUtils.trimPriceWithTicksize(price, tickSize, RoundingMode.UP).doubleValue());
		assertEquals(new BigDecimal("10.1").doubleValue(), PriceUtils.trimPriceWithTicksize(price, tickSize, RoundingMode.DOWN).doubleValue());
		
		price = new BigDecimal("10.11");
		tickSize = new BigDecimal("0.1");
		assertEquals(new BigDecimal("10.2").doubleValue(), PriceUtils.trimPriceWithTicksize(price, tickSize, RoundingMode.UP).doubleValue());
		assertEquals(new BigDecimal("10.1").doubleValue(), PriceUtils.trimPriceWithTicksize(price, tickSize, RoundingMode.DOWN).doubleValue());

		price = new BigDecimal("10.11111111");
		tickSize = new BigDecimal("0.1");
		assertEquals(new BigDecimal("10.2").doubleValue(), PriceUtils.trimPriceWithTicksize(price, tickSize, RoundingMode.UP).doubleValue());
		assertEquals(new BigDecimal("10.1").doubleValue(), PriceUtils.trimPriceWithTicksize(price, tickSize, RoundingMode.DOWN).doubleValue());

		price = new BigDecimal("1000.077795");
		tickSize = new BigDecimal("0.05");
		assertEquals(new BigDecimal("1000.1").doubleValue(), PriceUtils.trimPriceWithTicksize(price, tickSize, RoundingMode.UP).doubleValue());
		assertEquals(new BigDecimal("1000.05").doubleValue(), PriceUtils.trimPriceWithTicksize(price, tickSize, RoundingMode.DOWN).doubleValue());
	}
	
	@Test
	public void getStopProfitPriceTest() {
		BigDecimal price = new BigDecimal("1234.5678");
		BigDecimal stopProfitValue = new BigDecimal("12.321");
		StopProfitType stopProfitType = StopProfitType.valueOf("FIX");
		BigDecimal stopProfitPrice = PriceUtils.getStopProfitPrice(price, stopProfitType, stopProfitValue);
		assertEquals(new BigDecimal("1246.8888").doubleValue(), stopProfitPrice.doubleValue());
		
		price = new BigDecimal("1001.23");
		stopProfitValue = new BigDecimal("0.022");
		stopProfitType = StopProfitType.valueOf("RATE");
		stopProfitPrice = PriceUtils.getStopProfitPrice(price, stopProfitType, stopProfitValue);
		assertEquals(new BigDecimal("1023.25706").doubleValue(), stopProfitPrice.doubleValue());
		
		stopProfitValue = new BigDecimal("1.022");
		stopProfitPrice = PriceUtils.getStopProfitPrice(price, stopProfitType, stopProfitValue);
		assertEquals(new BigDecimal("1023.25706").doubleValue(), stopProfitPrice.doubleValue());
	}
}
