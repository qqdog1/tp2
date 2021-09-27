package name.qd.tp2.exchanges.Fake;

public class StateController {
	public static String UP = "UP";
	public static String DOWN = "DOWN";
	// 震盪
	public static String CORRECTION = "CORRECTION";
	
	private String[] states;
	private int[] times;
	
	private int index = 0;
	private int time = 0;
	
	public StateController(String[] states, int[] times) {
		this.states = states;
		this.times = times;
	}
	
	public String next() {
		time++;
		if(time > times[index]) {
			time = 1;
			index++;
			if(index >= states.length) {
				index = 0;
			}
		}
		return states[index];
	}
}
