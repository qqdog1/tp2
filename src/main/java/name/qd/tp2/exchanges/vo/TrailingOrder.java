package name.qd.tp2.exchanges.vo;

import name.qd.tp2.constants.TrailingStatus;

public class TrailingOrder extends Order {
	private TrailingStatus trailingStatus = TrailingStatus.TRAILING_STATUS_NONE;
	private double edgePrice;
	
	public TrailingStatus getTrailingStatus() {
		return trailingStatus;
	}
	public void setTrailingStatus(TrailingStatus trailingStatus) {
		this.trailingStatus = trailingStatus;
	}
	public double getEdgePrice() {
		return edgePrice;
	}
	public void setEdgePrice(double edgePrice) {
		this.edgePrice = edgePrice;
	}
}
