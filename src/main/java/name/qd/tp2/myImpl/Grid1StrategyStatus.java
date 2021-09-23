package name.qd.tp2.myImpl;

import java.io.IOException;

import name.qd.fileCache.cache.NormalObject;
import name.qd.fileCache.common.TransInputStream;
import name.qd.fileCache.common.TransOutputStream;

public class Grid1StrategyStatus extends NormalObject {
	private double firstOrderPrice;
	private int position;
	private double averagePrice;
	
	public double getFirstOrderPrice() {
		return firstOrderPrice;
	}
	public void setFirstOrderPrice(double firstOrderPrice) {
		this.firstOrderPrice = firstOrderPrice;
	}
	public int getPosition() {
		return position;
	}
	public void setPosition(int position) {
		this.position = position;
	}
	public double getAveragePrice() {
		return averagePrice;
	}
	public void setAveragePrice(double averagePrice) {
		this.averagePrice = averagePrice;
	}
	
	@Override
	public byte[] parseToFileFormat() throws IOException {
		TransOutputStream tOut = new TransOutputStream();
		tOut.writeDouble(firstOrderPrice);
		tOut.writeInt(position);
		tOut.writeDouble(averagePrice);
		return tOut.toByteArray();
	}
	@Override
	public void toValueObject(byte[] b) throws IOException {
		TransInputStream tIn = new TransInputStream(b);
		firstOrderPrice = tIn.getDouble();
		position = tIn.getInt();
		averagePrice = tIn.getDouble();
	}
	@Override
	public String getKeyString() {
		// 只有一組cache 固定key
		return "grid1";
	}
}
