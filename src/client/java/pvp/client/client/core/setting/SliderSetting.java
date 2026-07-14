package pvp.client.client.core.setting;

public class SliderSetting extends Setting {
	private double value;
	private final double defaultValue;
	private final double min;
	private final double max;
	private final double step;
	private final String suffix;

	public SliderSetting(String id, String name, double defaultValue, double min, double max, double step, String suffix) {
		super(id, name);
		this.value = defaultValue;
		this.defaultValue = defaultValue;
		this.min = min;
		this.max = max;
		this.step = step;
		this.suffix = suffix;
	}

	public double get() {
		return this.value;
	}

	public float getF() {
		return (float) this.value;
	}

	public int getI() {
		return (int) Math.round(this.value);
	}

	public void set(double value) {
		double snapped = Math.round(value / this.step) * this.step;
		this.value = Math.clamp(snapped, this.min, this.max);
	}

	public void reset() {
		this.value = this.defaultValue;
	}

	public double min() {
		return this.min;
	}

	public double max() {
		return this.max;
	}

	public double step() {
		return this.step;
	}

	public String suffix() {
		return this.suffix;
	}

	public String display() {
		String num = this.step >= 1.0 ? Integer.toString(this.getI()) : String.format("%.2f", this.value);
		return num + this.suffix;
	}
}
