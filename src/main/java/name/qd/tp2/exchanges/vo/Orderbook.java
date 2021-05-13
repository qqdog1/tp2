package name.qd.tp2.exchanges.vo;

import java.util.Comparator;
import java.util.TreeMap;

public class Orderbook {
	private TreeMap<Double, Double> bidBooks = new TreeMap<>(Comparator.reverseOrder());
	private TreeMap<Double, Double> askBooks = new TreeMap<>();
	
	public void addBid(double price, double qty) {
		add(bidBooks, price, qty);
	}
	
	public void addAsk(double price, double qty) {
		add(askBooks, price, qty);
	}
	
	public Double[] getBidTopPrice(int size) {
		return topPrice(bidBooks, size);
	}
	
	public Double[] getAskTopPrice(int size) {
		return topPrice(askBooks, size);
	}
	
	private void add(TreeMap<Double, Double> bookMap, double price, double qty) {
		double currentQty = 0;
		if(bookMap.containsKey(price)) {
			currentQty = bookMap.get(price);
		}
		currentQty += qty;
		bookMap.put(price, currentQty);
	}
	
	private Double[] topPrice(TreeMap<Double, Double> bookMap, int size) {
		return bookMap.keySet().stream().limit(size).toArray(price -> new Double[price]);
	}
}
