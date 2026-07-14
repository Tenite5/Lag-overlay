package pvp.client.client.modules;

import java.util.List;
import net.minecraft.client.Minecraft;
import pvp.client.client.core.setting.SliderSetting;
import pvp.client.client.prediction.LagPredictionManager;

public final class LagGhostModule {
	public static final LagGhostModule INSTANCE = new LagGhostModule();

	public final SliderSetting duration = new SliderSetting("duration", "Confidence Fade", 900.0, 100.0, 5000.0, 50.0, " ms");
	public final SliderSetting activationDistance = new SliderSetting("activationDistance", "Activation Gap", 0.25, 0.05, 4.0, 0.05, " blocks");
	public final SliderSetting opacity = new SliderSetting("opacity", "Ghost Opacity", 0.70, 0.05, 1.0, 0.05, "");
	public final SliderSetting minimumOpacity = new SliderSetting("minimumOpacity", "Min Confidence", 0.35, 0.0, 0.90, 0.05, "");
	public final SliderSetting reconciliationTime = new SliderSetting("reconciliationSpeed", "Rejoin Time", 0.10, 0.10, 3.0, 0.05, " s");
	public final SliderSetting detectionDelay = new SliderSetting("detectionDelay", "Packet Delay", 0.50, 0.40, 4.0, 0.05, "x");
	public final SliderSetting motionSamples = new SliderSetting("motionSamples", "Motion Samples", 2.0, 1.0, 8.0, 1.0, "");
	public final SliderSetting directionStability = new SliderSetting("directionStability", "Momentum Quality", 0.50, 0.0, 1.0, 0.05, "");

	private final List<SliderSetting> settings = List.of(
		this.activationDistance,
		this.detectionDelay,
		this.motionSamples,
		this.directionStability,
		this.opacity,
		this.minimumOpacity,
		this.duration,
		this.reconciliationTime
	);
	private boolean enabled = true;

	private LagGhostModule() {
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		if (this.enabled && !enabled) {
			LagPredictionManager.clear();
		}
		this.enabled = enabled;
	}

	public List<SliderSetting> settings() {
		return this.settings;
	}

	public void reset() {
		this.enabled = true;
		for (SliderSetting setting : this.settings) {
			setting.reset();
		}
	}

	public void tick(Minecraft minecraft) {
		if (this.enabled) {
			LagPredictionManager.tick(minecraft, this);
		}
	}
}
