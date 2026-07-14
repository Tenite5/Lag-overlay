package pvp.client.client.prediction;

public final class GhostRenderContext {
	private static final ThreadLocal<Float> OPACITY = ThreadLocal.withInitial(() -> 0.0F);

	private GhostRenderContext() {
	}

	public static boolean active() {
		return OPACITY.get() > 0.0F;
	}

	public static float opacity() {
		return OPACITY.get();
	}

	public static void run(float opacity, Runnable action) {
		OPACITY.set(opacity);
		try {
			action.run();
		} finally {
			OPACITY.remove();
		}
	}
}
