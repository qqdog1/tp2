package name.qd.tp2.exchanges.vo;

import name.qd.tp2.constants.BuySell;

public class Order {
	private BuySell buySell;
	private double price;
	private double qty;
	public Order(BuySell buySell, double price, double qty) {
		this.buySell = buySell;
		this.price = price;
		this.qty = qty;
	}
	public BuySell getBuySell() {
		return buySell;
	}
	public void setBuySell(BuySell buySell) {
		this.buySell = buySell;
	}
	public double getPrice() {
		return price;
	}
	public void setPrice(double price) {
		this.price = price;
	}
	public double getQty() {
		return qty;
	}
	public void setQty(double qty) {
		this.qty = qty;
	}
}
