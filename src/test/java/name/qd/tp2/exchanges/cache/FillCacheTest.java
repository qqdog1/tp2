package name.qd.tp2.exchanges.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import name.qd.tp2.exchanges.vo.Fill;

public class FillCacheTest {
	private FillCache fillCache;
	
	@BeforeEach
	void init() {
		fillCache = new FillCache();
	}
	
	@Test
	public void getNullCache() {
		List<Fill> lst = fillCache.getFill("test");
		assertEquals(lst.size(), 0);
	}

	@Test
	public void pushAndPop() {
		Fill fillA = new Fill();
		fillA.setUserName("AA");
		fillA.setFillPrice("123");
		fillA.setQty("0.02");
		fillA.setSymbol("BTC");
		
		Fill fillB = new Fill();
		fillB.setUserName("BB");
		fillB.setFillPrice("321");
		fillB.setQty("0.111");
		fillB.setSymbol("ETH");
		
		// add A
		fillCache.addFill("TEST", fillA);
		// add B
		fillCache.addFill("TEST", fillB);
		// should get A
		List<Fill> lst = fillCache.getFill("TEST");
		Fill fill = lst.get(0);
		assertTrue(fill.getUserName().equals(fillA.getUserName()));
		assertTrue(fill.getFillPrice().equals(fillA.getFillPrice()));
		assertTrue(fill.getQty().equals(fillA.getQty()));
		assertTrue(fill.getSymbol() == fillA.getSymbol());
		// should get B
		fill = lst.get(1);
		assertTrue(fill.getUserName().equals(fillB.getUserName()));
		assertTrue(fill.getFillPrice().equals(fillB.getFillPrice()));
		assertTrue(fill.getQty().equals(fillB.getQty()));
		assertTrue(fill.getSymbol().equals(fillB.getSymbol()));
	}
}
