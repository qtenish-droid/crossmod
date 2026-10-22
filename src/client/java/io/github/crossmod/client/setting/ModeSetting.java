package io.github.crossmod.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Arrays;
import java.util.List;

/**
 * A setting that picks one of a fixed list of options.
 */
public final class ModeSetting extends Setting<String> {

	private final List<String> options;

	public ModeSetting(String id, String name, String description, String defaultValue, String... options) {
		super(id, name, description, defaultValue);
		this.options = List.copyOf(Arrays.asList(options));
	}

	/**
	 * Returns the available options.
	 *
	 * @return the options
	 */
	public List<String> options() {
		return options;
	}

	/**
	 * Returns the index of the currently selected option.
	 *
	 * @return the index, or {@code -1} when the value is unknown
	 */
	public int index() {
		return options.indexOf(value());
	}

	/**
	 * Selects the option at the given index.
	 *
	 * @param index the index of the option
	 */
	public void select(int index) {
		if (index >= 0 && index < options.size()) {
			setValue(options.get(index));
		}
	}

	/**
	 * Selects the next option, wrapping around at the end.
	 */
	public void cycle() {
		if (options.isEmpty()) {
			return;
		}

		int index = index();

		select(index < 0 || index + 1 >= options.size() ? 0 : index + 1);
	}

	@Override
	public JsonElement write() {
		return new JsonPrimitive(value());
	}

	@Override
	public void read(JsonElement element) {
		if (element != null && element.isJsonPrimitive()) {
			String stored = element.getAsString();

			if (options.contains(stored)) {
				setValue(stored);
			}
		}
	}
}
