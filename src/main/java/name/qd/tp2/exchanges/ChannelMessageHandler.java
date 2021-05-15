package name.qd.tp2.exchanges;

import java.util.concurrent.ConcurrentLinkedDeque;

public abstract class ChannelMessageHandler implements Runnable {
	private final ConcurrentLinkedDeque<String> queue = new ConcurrentLinkedDeque<>();
	
	public void onMessage(String text) {
		queue.offer(text);
	}
	
	@Override
	public void run() {
		String message;
		while (!Thread.currentThread().isInterrupted()) {
			if((message = queue.poll()) != null) {
				// TODO 如果有哪個交易所丟訊息快到我策略無法處裡
				// 可以在這邊把queue清空
//				queue.clear();
				processMessage(message);
			}
		}	
	}
	
	public abstract void processMessage(String text);
}
