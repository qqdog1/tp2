package name.qd.tp2.exchanges.BTSE;

import org.junit.jupiter.api.BeforeEach;

public class BTSEExchangeTest {
	private BTSEFuturesExchange exchange;
	
	@BeforeEach
	void init() {
		exchange = new BTSEFuturesExchange();
	}
}
