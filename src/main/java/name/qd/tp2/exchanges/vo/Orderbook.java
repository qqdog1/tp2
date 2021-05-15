package name.qd.tp2.exchanges.vo;

import java.util.Comparator;
import java.util.TreeMap;

public class Orderbook implements Cloneable {
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
	
	public Double[] getBidTopQty(int size) {
		return topQty(bidBooks, size);
	}
	
	public Double[] getAskTopPrice(int size) {
		return topPrice(askBooks, size);
	}
	
	public Double[] getAskTopQty(int size) {
		return topQty(bidBooks, size);
	}

	private void add(TreeMap<Double, Double> bookMap, double price, double qty) {
		double currentQty = 0;
		if (bookMap.containsKey(price)) {
			currentQty = bookMap.get(price);
		}
		currentQty += qty;
		bookMap.put(price, currentQty);
	}

	private Double[] topPrice(TreeMap<Double, Double> bookMap, int size) {
		return bookMap.keySet().stream().limit(size).toArray(price -> new Double[price]);
	}
	
	private Double[] topQty(TreeMap<Double, Double> bookMap, int size) {
		return bookMap.values().stream().limit(size).toArray(qty -> new Double[qty]);
	}

	public Orderbook clone() throws CloneNotSupportedException {
		return (Orderbook)super.clone();
	}
}
