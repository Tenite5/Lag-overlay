package pvp.client.client.core.setting;

public abstract class Setting {
	private final String id;
	private final String name;

	protected Setting(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public String id() {
		return this.id;
	}

	public String name() {
		return this.name;
	}
}
