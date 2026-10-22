package io.github.crossmod.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * A setting that is either enabled or disabled.
 */
public final class BooleanSetting extends Setting<Boolean> {

	public BooleanSetting(String id, String name, String description, boolean defaultValue) {
		super(id, name, description, defaultValue);
	}

	/**
	 * Flips the value of the setting.
	 */
	public void toggle() {
		setValue(!value());
	}

	@Override
	public JsonElement write() {
		return new JsonPrimitive(value());
	}

	@Override
	public void read(JsonElement element) {
		if (element != null && element.isJsonPrimitive()) {
			setValue(element.getAsBoolean());
		}
	}
}
