package name.qd.tp2.exchanges.cache;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
	public void pushAndPop() {
		Fill fillA = new Fill();
		fillA.setUserName("AA");
		fillA.setPrice("123");
		fillA.setQty("0.02");
		fillA.setSymbol("BTC");
		
		Fill fillB = new Fill();
		fillB.setUserName("BB");
		fillB.setPrice("321");
		fillB.setQty("0.111");
		fillB.setSymbol("ETH");
		
		// offer A
		fillCache.offerFill("TEST", fillA);
		// offer B
		fillCache.offerFill("TEST", fillB);
		// pop should get A
		Fill fill = fillCache.popFill("TEST");
		assertTrue(fill.getUserName().equals(fillA.getUserName()));
		assertTrue(fill.getPrice().equals(fillA.getPrice()));
		assertTrue(fill.getQty().equals(fillA.getQty()));
		assertTrue(fill.getSymbol().equals(fillA.getSymbol()));
		// pop should get B
		fill = fillCache.popFill("TEST");
		assertTrue(fill.getUserName().equals(fillB.getUserName()));
		assertTrue(fill.getPrice().equals(fillB.getPrice()));
		assertTrue(fill.getQty().equals(fillB.getQty()));
		assertTrue(fill.getSymbol().equals(fillB.getSymbol()));
		
		// pop should get null
		fill = fillCache.popFill("TEST");
		assertTrue(fill == null);
	}
}
