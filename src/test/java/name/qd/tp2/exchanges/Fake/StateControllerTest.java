package name.qd.tp2.exchanges.Fake;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class StateControllerTest {

	@Test
	public void stateController() {
		String[] states = new String[] {StateController.UP, StateController.CORRECTION, StateController.DOWN};
		int[] times = new int[] {5, 4, 3};
		StateController stateController = new StateController(states, times);
		
		assertEquals(stateController.next(), StateController.UP);
		assertEquals(stateController.next(), StateController.UP);
		assertEquals(stateController.next(), StateController.UP);
		assertEquals(stateController.next(), StateController.UP);
		assertEquals(stateController.next(), StateController.UP);
		
		assertEquals(stateController.next(), StateController.CORRECTION);
		assertEquals(stateController.next(), StateController.CORRECTION);
		assertEquals(stateController.next(), StateController.CORRECTION);
		assertEquals(stateController.next(), StateController.CORRECTION);
		
		assertEquals(stateController.next(), StateController.DOWN);
		assertEquals(stateController.next(), StateController.DOWN);
		assertEquals(stateController.next(), StateController.DOWN);
		
		assertEquals(stateController.next(), StateController.UP);
		assertEquals(stateController.next(), StateController.UP);
		assertEquals(stateController.next(), StateController.UP);
		assertEquals(stateController.next(), StateController.UP);
		assertEquals(stateController.next(), StateController.UP);
		
		assertEquals(stateController.next(), StateController.CORRECTION);
		assertEquals(stateController.next(), StateController.CORRECTION);
		assertEquals(stateController.next(), StateController.CORRECTION);
		assertEquals(stateController.next(), StateController.CORRECTION);
		
		assertEquals(stateController.next(), StateController.DOWN);
		assertEquals(stateController.next(), StateController.DOWN);
		assertEquals(stateController.next(), StateController.DOWN);
	}
}
