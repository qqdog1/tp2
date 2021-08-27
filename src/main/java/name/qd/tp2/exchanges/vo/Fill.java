package name.qd.tp2.exchanges.vo;

import name.qd.tp2.constants.BuySell;

public class Fill {
	private String userName;
	private String orderId;
	private String symbol;
	private BuySell buySell;
	private String fillPrice;
	private String orderPrice;
	private String qty;
	private long timestamp;
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getOrderId() {
		return orderId;
	}
	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}
	public String getSymbol() {
		return symbol;
	}
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	public String getFillPrice() {
		return fillPrice;
	}
	public void setFillPrice(String fillPrice) {
		this.fillPrice = fillPrice;
	}
	public String getOrderPrice() {
		return orderPrice;
	}
	public void setOrderPrice(String orderPrice) {
		this.orderPrice = orderPrice;
	}
	public String getQty() {
		return qty;
	}
	public void setQty(String qty) {
		this.qty = qty;
	}
	public BuySell getBuySell() {
		return buySell;
	}
	public void setBuySell(BuySell buySell) {
		this.buySell = buySell;
	}
	public long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
}
