package pvp.client.client.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import pvp.client.client.core.ConfigManager;
import pvp.client.client.core.setting.SliderSetting;
import pvp.client.client.modules.LagGhostModule;

public final class LagGhostSettingsScreen extends OptionsSubScreen {
	private static final Component TITLE = Component.translatable("screen.predicted-lag-overlay.lag_ghost.title");
	private final List<SettingSlider> sliders = new ArrayList<>();
	private CycleButton<Boolean> enabledButton;

	public LagGhostSettingsScreen(Screen parent) {
		super(parent, net.minecraft.client.Minecraft.getInstance().options, TITLE);
	}

	@Override
	protected void addOptions() {
		LagGhostModule module = LagGhostModule.INSTANCE;
		this.enabledButton = CycleButton.onOffBuilder(module.isEnabled())
			.create(Component.translatable("screen.predicted-lag-overlay.lag_ghost.enabled"), (button, enabled) -> module.setEnabled(enabled));
		this.enabledButton.setTooltip(Tooltip.create(Component.literal("Master switch for visual lag prediction.")));
		this.list.addBig(this.enabledButton);
		this.list.addHeader(Component.translatable("screen.predicted-lag-overlay.lag_ghost.detection"));
		this.list.addSmall(List.of(
			this.slider(module.activationDistance, "Distance between the frozen player and prediction before the ghost appears. Lower activates sooner."),
			this.slider(module.detectionDelay, "Multiplier for the learned packet-delay threshold. Lower detects a freeze sooner."),
			this.slider(module.motionSamples, "Recent moving updates required before extrapolation. More samples reduce false activations."),
			this.slider(module.directionStability, "Required consistency of recent movement speed. Turning does not count against normal movement.")
		));

		this.list.addHeader(Component.translatable("screen.predicted-lag-overlay.lag_ghost.visuals"));
		this.list.addSmall(List.of(
			this.slider(module.opacity, "Maximum opacity when a prediction first activates."),
			this.slider(module.minimumOpacity, "Lowest confidence and opacity retained during a long stall."),
			this.slider(module.duration, "Time for prediction confidence to fade toward its minimum."),
			this.slider(module.reconciliationTime, "Time for the ghost to mostly rejoin the real player. Lower is faster.")
		));
	}

	private SettingSlider slider(SliderSetting setting, String tooltip) {
		SettingSlider slider = new SettingSlider(setting);
		slider.setTooltip(Tooltip.create(Component.literal(tooltip)));
		this.sliders.add(slider);
		return slider;
	}

	@Override
	protected void addFooter() {
		LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
		footer.addChild(Button.builder(Component.translatable("screen.predicted-lag-overlay.lag_ghost.reset"), button -> this.resetDefaults()).width(150).build());
		footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).width(150).build());
	}

	private void resetDefaults() {
		LagGhostModule.INSTANCE.reset();
		if (this.enabledButton != null) {
			this.enabledButton.setValue(true);
		}
		for (SettingSlider slider : this.sliders) {
			slider.resetToDefault();
		}
		ConfigManager.save();
	}

	@Override
	public void onClose() {
		ConfigManager.save();
		super.onClose();
	}

	private static final class SettingSlider extends AbstractSliderButton {
		private final SliderSetting setting;

		private SettingSlider(SliderSetting setting) {
			super(0, 0, 150, 20, Component.empty(), fraction(setting));
			this.setting = setting;
			this.updateMessage();
		}

		private static double fraction(SliderSetting setting) {
			return (setting.get() - setting.min()) / (setting.max() - setting.min());
		}

		@Override
		protected void updateMessage() {
			this.setMessage(CommonComponents.optionNameValue(Component.literal(this.setting.name()), Component.literal(this.setting.display())));
		}

		@Override
		protected void applyValue() {
			this.setting.set(this.setting.min() + this.value * (this.setting.max() - this.setting.min()));
			this.value = fraction(this.setting);
		}

		private void resetToDefault() {
			this.setting.reset();
			this.value = fraction(this.setting);
			this.updateMessage();
		}
	}
}
