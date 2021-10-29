package name.qd.tp2.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

import name.qd.tp2.constants.StopProfitType;

public class PriceUtils {
	// trim with tick size
	public static BigDecimal trimPriceWithTicksize(BigDecimal price, BigDecimal tickSize, RoundingMode roundingMode) {
		BigDecimal diff = price.remainder(tickSize);
		
		if(diff.compareTo(BigDecimal.ZERO) == 0) {
			return price;
		}
		
		switch (roundingMode) {
		case UP:
			// 往上貼近下一個tick
			// ex: price:30.7, ticksize:0.5
			// result: 31
			price = price.subtract(diff).add(tickSize);
			break;
		case DOWN:
			// 往下貼近下一個tick
			// ex: price:30.7, ticksize:0.5
			// result: 30.5
			price = price.subtract(diff);
			break;
		default:
			break;
		}
		return price;
	}
	
	public static BigDecimal getStopProfitPrice(BigDecimal price, StopProfitType stopProfitType, BigDecimal stopProfitValue) {
		BigDecimal stopProfitPrice = price;
		switch (stopProfitType) {
			case FIX:
				stopProfitPrice = price.add(stopProfitValue);
				break;
			case RATE:
				if(stopProfitValue.compareTo(BigDecimal.ONE) == -1) {
					// stopProfitValue < 1
					stopProfitValue = stopProfitValue.add(BigDecimal.ONE);
				} 
				stopProfitPrice = price.multiply(stopProfitValue);
				break;
			default:
				break;
		}
		return stopProfitPrice;
	}

	public static boolean isMeetProfitTrailingStop(BigDecimal profitPrice, BigDecimal currentPrice,
												   BigDecimal maxPrice, BigDecimal pullback) {
		if (profitPrice.compareTo(currentPrice) == 1) {
			return false;
		}
		if (maxPrice.subtract(pullback).compareTo(currentPrice) == -1) {
			return false;
		}
		if (maxPrice.subtract(pullback).compareTo(profitPrice) == -1) {
			return false;
		}
		return true;
	}
}
