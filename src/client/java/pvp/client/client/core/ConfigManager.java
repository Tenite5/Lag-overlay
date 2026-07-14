package pvp.client.client.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import pvp.client.PredictedLagOverlay;
import pvp.client.client.core.setting.SliderSetting;
import pvp.client.client.modules.LagGhostModule;

public final class ConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private ConfigManager() {
	}

	private static Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve("predicted-lag-overlay.json");
	}

	private static Path legacyConfigPath() {
		return FabricLoader.getInstance().getConfigDir().resolve("pvp-client.json");
	}

	public static void save() {
		LagGhostModule module = LagGhostModule.INSTANCE;
		JsonObject root = new JsonObject();
		root.addProperty("enabled", module.isEnabled());
		JsonObject settings = new JsonObject();
		for (SliderSetting setting : module.settings()) {
			settings.addProperty(setting.id(), setting.get());
		}
		root.add("settings", settings);
		try {
			Files.writeString(configPath(), GSON.toJson(root));
		} catch (IOException e) {
			PredictedLagOverlay.LOGGER.error("Failed to save config", e);
		}
	}

	public static void load() {
		Path currentPath = configPath();
		boolean legacy = !Files.exists(currentPath) && Files.exists(legacyConfigPath());
		Path path = legacy ? legacyConfigPath() : currentPath;
		if (!Files.exists(path)) {
			return;
		}

		JsonObject root;
		try {
			root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
		} catch (Exception e) {
			PredictedLagOverlay.LOGGER.error("Failed to load config", e);
			return;
		}

		LagGhostModule module = LagGhostModule.INSTANCE;
		JsonObject entry = root;
		boolean combinedConfig = root.has("modules");
		if (combinedConfig) {
			JsonElement oldEntry = root.getAsJsonObject("modules").get("lagghost");
			if (oldEntry == null || !oldEntry.isJsonObject()) {
				return;
			}
			entry = oldEntry.getAsJsonObject();
		}
		if (entry.has("enabled")) {
			module.setEnabled(entry.get("enabled").getAsBoolean());
		}
		if (legacy || combinedConfig || !entry.has("settings")) {
			return;
		}

		JsonObject settings = entry.getAsJsonObject("settings");
		for (SliderSetting setting : module.settings()) {
			JsonElement value = settings.get(setting.id());
			if (value != null) {
				try {
					setting.set(value.getAsDouble());
				} catch (Exception e) {
					PredictedLagOverlay.LOGGER.warn("Bad config value for {}", setting.id());
				}
			}
		}
	}
}
