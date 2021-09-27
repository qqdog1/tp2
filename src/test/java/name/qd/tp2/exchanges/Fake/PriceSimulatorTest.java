package name.qd.tp2.exchanges.Fake;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class PriceSimulatorTest {

	@Test
	public void priceTest() {
		String[] states = new String[] {StateController.UP, StateController.DOWN};
		int[] times = new int[] {3, 4};
		PriceSimulator priceSimulator = new PriceSimulator(3000, states, times);
		
		assertEquals(priceSimulator.next(), 3001);
		assertEquals(priceSimulator.next(), 3002);
		assertEquals(priceSimulator.next(), 3003);
		
		assertEquals(priceSimulator.next(), 3002);
		assertEquals(priceSimulator.next(), 3001);
		assertEquals(priceSimulator.next(), 3000);
		assertEquals(priceSimulator.next(), 2999);
		
		assertEquals(priceSimulator.next(), 3000);
		assertEquals(priceSimulator.next(), 3001);
		assertEquals(priceSimulator.next(), 3002);
		
		assertEquals(priceSimulator.next(), 3001);
		assertEquals(priceSimulator.next(), 3000);
		assertEquals(priceSimulator.next(), 2999);
		assertEquals(priceSimulator.next(), 2998);
	}
}
