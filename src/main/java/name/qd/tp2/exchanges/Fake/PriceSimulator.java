package name.qd.tp2.exchanges.Fake;

public class PriceSimulator {
	private final StateController stateController;
	private double startPrice;
	
	public PriceSimulator(double startPrice, String[] states, int[] times) {
		stateController = new StateController(states, times);
		this.startPrice = startPrice;
	}
	
	public double next() {
		String nextState = stateController.next();
		if(StateController.UP.equals(nextState)) {
			startPrice++;
		} else if(StateController.DOWN.equals(nextState)) {
			startPrice--;
		} else if(StateController.CORRECTION.equals(nextState)) {
			// TODO 太麻煩先用UP DOWN
		}
		return startPrice;
	}
}
