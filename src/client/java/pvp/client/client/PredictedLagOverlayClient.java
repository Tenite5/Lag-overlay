package pvp.client.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import pvp.client.PredictedLagOverlay;
import pvp.client.client.core.ConfigManager;
import pvp.client.client.gui.LagGhostSettingsScreen;
import pvp.client.client.modules.LagGhostModule;

public final class PredictedLagOverlayClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(PredictedLagOverlay.id("settings"));
	private static final KeyMapping SETTINGS_KEY = KeyMappingHelper.registerKeyMapping(
		new KeyMapping("key.predicted-lag-overlay.settings", InputConstants.KEY_K, CATEGORY)
	);

	@Override
	public void onInitializeClient() {
		ConfigManager.load();

		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			LagGhostModule.INSTANCE.tick(minecraft);
			while (SETTINGS_KEY.consumeClick()) {
				if (minecraft.gui.screen() == null) {
					minecraft.gui.setScreen(new LagGhostSettingsScreen(null));
				}
			}
		});

		ClientLifecycleEvents.CLIENT_STOPPING.register(minecraft -> ConfigManager.save());
		PredictedLagOverlay.LOGGER.info("Predicted Lag Overlay loaded");
	}
}
