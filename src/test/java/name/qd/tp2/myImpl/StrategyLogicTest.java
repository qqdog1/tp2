package name.qd.tp2.myImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

public class StrategyLogicTest {
	private int g2OrderLevel = 5;
	private double g2PriceRange = 3;
	private Set<Double> setG2OpenPrice = new HashSet<>();
	
	
	@Test
	public void placeOrderTest() {
		BigDecimal price = new BigDecimal("3211");
		// 3210 3207 3204 3201 3198
		placeG2LevelOrder(price);
		
		assertEquals(5, setG2OpenPrice.size());
		assertTrue(setG2OpenPrice.contains(3210d));
		assertTrue(setG2OpenPrice.contains(3207d));
		assertTrue(setG2OpenPrice.contains(3204d));
		assertTrue(setG2OpenPrice.contains(3201d));
		assertTrue(setG2OpenPrice.contains(3198d));
		
		
		price = new BigDecimal("3217");
		// 3216 3213 3210 3207 3204
		placeG2LevelOrder(price);
		
		assertEquals(7, setG2OpenPrice.size());
		assertTrue(setG2OpenPrice.contains(3216d));
		assertTrue(setG2OpenPrice.contains(3213d));
		assertTrue(setG2OpenPrice.contains(3210d));
		assertTrue(setG2OpenPrice.contains(3207d));
		assertTrue(setG2OpenPrice.contains(3204d));
		assertTrue(setG2OpenPrice.contains(3201d));
		assertTrue(setG2OpenPrice.contains(3198d));
		
		
		cancelFarG2OpenOrder();
		
		assertEquals(5, setG2OpenPrice.size());
		assertTrue(setG2OpenPrice.contains(3216d));
		assertTrue(setG2OpenPrice.contains(3213d));
		assertTrue(setG2OpenPrice.contains(3210d));
		assertTrue(setG2OpenPrice.contains(3207d));
		assertTrue(setG2OpenPrice.contains(3204d));
	}
	
	private void placeG2LevelOrder(BigDecimal basePrice) {
		basePrice = basePrice.subtract(basePrice.remainder(BigDecimal.valueOf(g2PriceRange)));
		for(int i = 0 ; i < g2OrderLevel ;) {
			BigDecimal price = basePrice.subtract(BigDecimal.valueOf(g2PriceRange).multiply(BigDecimal.valueOf(i)));
			if(!setG2OpenPrice.contains(price.doubleValue())) {
				setG2OpenPrice.add(price.doubleValue());
				i++;
			} else {
				i++;
			}
		}
	}
	
	private void cancelFarG2OpenOrder() {
		LinkedHashSet<Double> set = setG2OpenPrice.stream().sorted().collect(Collectors.toCollection(LinkedHashSet::new));
		Double[] da = new Double[set.size()];
		set.toArray(da);
		// 由小到大
		
		if(setG2OpenPrice.size() > g2OrderLevel) {
			List<Double> lstRemovePrice = new ArrayList<>();
			for(int i = 0 ; i < da.length - g2OrderLevel; i++) {
				lstRemovePrice.add(da[i]);
			}
			
			for(Double d : lstRemovePrice) {
				setG2OpenPrice.remove(d);
			}
		}
	}
}
