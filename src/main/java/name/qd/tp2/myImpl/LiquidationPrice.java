package name.qd.tp2.myImpl;

public class LiquidationPrice {

	public static void main(String[] args) {
		double balance = 10000;
		double currentPrice = 3000;
		double minMarketPrice = 0;
		double priceRange = 3;
		double contractSize = 1;
		
		double cost = 0;
		double position = 1;
		
		for(int i = 0 ;; i++, position++) {
			double price = currentPrice - (priceRange * i);
			if(price <= minMarketPrice) break;
			cost += price * contractSize;
			double avgPrice = cost / position;
			
			if(avgPrice - (balance / position * 100) > price) {
				System.out.println("liquidation price: " + (avgPrice - (balance / position * 100)) + 
						" , price: " + price + " , qty: " + position);
				break;
			}
		}
	}

}
