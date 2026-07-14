package pvp.client.client.prediction;

public final class NetworkTiming {
	private static volatile long lastInboundNanos = System.nanoTime();

	private NetworkTiming() {
	}

	public static void inboundPacket() {
		lastInboundNanos = System.nanoTime();
	}

	public static long silenceNanos(long now) {
		return Math.max(0L, now - lastInboundNanos);
	}
}
