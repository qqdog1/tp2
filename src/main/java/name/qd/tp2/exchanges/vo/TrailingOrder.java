package name.qd.tp2.exchanges.vo;

public class TrailingOrder extends Order {
	public static int TRAILING_STATUS_NONE = 0;
	public static int TRAILING_STATUS_TRIIGERED = 1;
	public static int TRAILING_STATUS_SENDED = 2;
	
	private int trailingStatus = TRAILING_STATUS_NONE;
	private double edgePrice;
	
	public int getTrailingStatus() {
		return trailingStatus;
	}
	public void setTrailingStatus(int trailingStatus) {
		this.trailingStatus = trailingStatus;
	}
	public double getEdgePrice() {
		return edgePrice;
	}
	public void setEdgePrice(double edgePrice) {
		this.edgePrice = edgePrice;
	}
}
