package name.qd.tp2.exchanges.cache;

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
	public void pushAndPop() {
		Fill fillA = new Fill();
		fillA.setUserName("AA");
		fillA.setPrice(123d);
		fillA.setQty(0.02d);
		fillA.setSymbol("BTC");
		
		Fill fillB = new Fill();
		fillB.setUserName("BB");
		fillB.setPrice(321d);
		fillB.setQty(0.111d);
		fillB.setSymbol("ETH");
		
		// offer A
		fillCache.addFill("TEST", fillA);
		// offer B
		fillCache.addFill("TEST", fillB);
		// pop should get A
//		List<Fill> fill = fillCache.getFill("TEST");
//		assertTrue(fill.getUserName().equals(fillA.getUserName()));
//		assertTrue(fill.getPrice().equals(fillA.getPrice()));
//		assertTrue(fill.getQty().equals(fillA.getQty()));
//		assertTrue(fill.getSymbol().equals(fillA.getSymbol()));
//		// pop should get B
//		fill = fillCache.popFill("TEST");
//		assertTrue(fill.getUserName().equals(fillB.getUserName()));
//		assertTrue(fill.getPrice().equals(fillB.getPrice()));
//		assertTrue(fill.getQty().equals(fillB.getQty()));
//		assertTrue(fill.getSymbol().equals(fillB.getSymbol()));
//		
//		// pop should get null
//		fill = fillCache.popFill("TEST");
//		assertTrue(fill == null);
	}
}
