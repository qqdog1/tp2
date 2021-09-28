package name.qd.tp2.myImpl.vo;

import java.io.IOException;

import name.qd.fileCache.cache.NormalObject;
import name.qd.fileCache.common.TransInputStream;
import name.qd.fileCache.common.TransOutputStream;
import name.qd.tp2.constants.BuySell;
import name.qd.tp2.exchanges.vo.Fill;

public class FillCacheByPrice extends NormalObject {
	private Fill fill;
	
	public FillCacheByPrice() {
		fill = new Fill();
	}
	
	public FillCacheByPrice(Fill fill) {
		this.fill = fill;
	}
	
	public Fill getFill() {
		return fill;
	}
	
	public BuySell getBuySell() {
		return fill.getBuySell();
	}
	
	public String getOrderId() {
		return fill.getOrderId();
	}
	
	public String getSymbol() {
		return fill.getSymbol();
	}
	
	public String getFillPrice() {
		return fill.getFillPrice();
	}
	
	public String getOrderPrice() {
		return fill.getOrderPrice();
	}
	
	public String getQty() {
		return fill.getQty();
	}
	
	public long getTimestamp() {
		return fill.getTimestamp();
	}
	
	public String getUserName() {
		return fill.getUserName();
	}
	
	public String getFee() {
		return fill.getFee();
	}

	@Override
	public byte[] parseToFileFormat() throws IOException {
		TransOutputStream tOut = new TransOutputStream();
		tOut.writeString(fill.getBuySell().name());
		tOut.writeString(fill.getOrderId());
		tOut.writeString(fill.getSymbol());
		tOut.writeString(fill.getFillPrice());
		tOut.writeString(fill.getOrderPrice());
		tOut.writeString(fill.getQty());
		tOut.writeLong(fill.getTimestamp());
		tOut.writeString(fill.getUserName());
		tOut.writeString(fill.getFee());
		return tOut.toByteArray();
	}

	@Override
	public void toValueObject(byte[] data) throws IOException {
		TransInputStream tIn = new TransInputStream(data);
		fill = new Fill();
		
		fill.setBuySell(BuySell.valueOf(tIn.getString()));
		fill.setOrderId(tIn.getString());
		fill.setSymbol(tIn.getString());
		fill.setFillPrice(tIn.getString());
		fill.setOrderPrice(tIn.getString());
		fill.setQty(tIn.getString());
		fill.setTimestamp(tIn.getLong());
		fill.setUserName(tIn.getString());
		fill.setFee(tIn.getString());
	}

	@Override
	public String getKeyString() {
		return fill.getOrderPrice();
	}
}
