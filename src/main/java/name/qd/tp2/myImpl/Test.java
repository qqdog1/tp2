package name.qd.tp2.myImpl;

public class Test {
	private static int position = 80;
	
	
	private int firstContractSize = 10;
	private int priceRange = 10;
	private int orderLevel = 5;
	
	public static void main(String[] s) {
		Test test = new Test();
//		System.out.println(test.calcCurrentAvgPrice(3000));
//		System.out.println(test.getRemainOrderLevel(position));
		test.placeLevelOrders(4, 3264);
	}
	
	private Test() {
	}
	
	private double calcCurrentAvgPrice(double firstOrderPrice) {
		double cost = firstOrderPrice * firstContractSize;
		int qty = position - firstContractSize;
		double price = firstOrderPrice;
		System.out.println(String.format("未平倉第%s單, price: %s, qty: %s", 1, firstOrderPrice, firstContractSize));
		for(int i = 1 ; qty > 0 ; i++) {
			price -= (priceRange * Math.pow(2, i-1));
			double size = firstContractSize * Math.pow(2, i-1);
			cost +=  price * size;
			qty -= size;
			System.out.println(String.format("未平倉第%s單, price: %s, qty: %s", i+1, price, size));
		}
		return cost / position;
	}
	
	private int getRemainOrderLevel(int remainQty) {
		if(remainQty < firstContractSize) {
			return 0;
		}
		int level = 0;
		while(remainQty > 0) {
			level++;
			remainQty -= firstContractSize * level;
		}
		return level;
	}
	
	private void placeLevelOrders(int startLevel, double basePrice) {
		for(int i = 1 ; i < orderLevel ; i++) {
			basePrice = basePrice - (priceRange * Math.pow(2, i-1));
			double qty = firstContractSize * Math.pow(2, i-1);
			if(i == startLevel) {
				System.out.println(String.format("order level %s, price: %s, qty: %s", i, basePrice, qty));
				startLevel++;
			}
		}
	}
}
