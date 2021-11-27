package name.qd.tp2.exchanges.vo;

import name.qd.tp2.constants.BuySell;

public class Order {
	private String strategyName;
	private String exchange;
	private String userName;
	private String symbol;
	private BuySell buySell;
	private double price;
	private double qty;
	public Order() {}
	public Order(BuySell buySell, double price, double qty) {
		this.buySell = buySell;
		this.price = price;
		this.qty = qty;
	}
	public String getStrategyName() {
		return strategyName;
	}
	public void setStrategyName(String strategyName) {
		this.strategyName = strategyName;
	}
	public String getExchange() {
		return exchange;
	}
	public void setExchange(String exchange) {
		this.exchange = exchange;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getSymbol() {
		return symbol;
	}
	public void setSymbol(String symbol) {
		this.symbol = symbol;
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
