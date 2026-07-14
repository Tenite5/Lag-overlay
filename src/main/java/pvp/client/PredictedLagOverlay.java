package pvp.client;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PredictedLagOverlay {
	public static final String MOD_ID = "predicted-lag-overlay";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private PredictedLagOverlay() {
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
