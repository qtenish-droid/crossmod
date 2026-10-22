package io.github.crossmod.client.setting;

import com.google.gson.JsonElement;

import java.util.function.Consumer;

/**
 * Base class of every configurable value of a module.
 *
 * <p>Settings know how to serialise themselves, which keeps the configuration
 * format completely decoupled from the user interface. The interface renders
 * the concrete types through {@code SettingType}.</p>
 *
 * @param <T> the type of the value
 */
public abstract class Setting<T> {

	private final String id;
	private final String name;
	private final String description;
	private final T defaultValue;

	private T value;
	private Consumer<T> onChange;

	protected Setting(String id, String name, String description, T defaultValue) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.defaultValue = defaultValue;
		this.value = defaultValue;
	}

	/**
	 * Returns the stable identifier of the setting.
	 *
	 * @return the identifier
	 */
	public String id() {
		return id;
	}

	/**
	 * Returns the human readable name of the setting.
	 *
	 * @return the name
	 */
	public String name() {
		return name;
	}

	/**
	 * Returns the description of the setting, shown as a tooltip.
	 *
	 * @return the description
	 */
	public String description() {
		return description;
	}

	/**
	 * Returns the default value of the setting.
	 *
	 * @return the default value
	 */
	public T defaultValue() {
		return defaultValue;
	}

	/**
	 * Returns the current value of the setting.
	 *
	 * @return the current value
	 */
	public T value() {
		return value;
	}

	/**
	 * Updates the value of the setting.
	 *
	 * @param value the new value
	 */
	public void setValue(T value) {
		if (this.value == null ? value != null : !this.value.equals(value)) {
			this.value = value;
			notifyChanged();
		}
	}

	/**
	 * Restores the default value of the setting.
	 */
	public void reset() {
		setValue(defaultValue);
	}

	/**
	 * Registers a listener that is invoked whenever the value changes.
	 *
	 * @param onChange the listener
	 */
	public void onChange(Consumer<T> onChange) {
		this.onChange = onChange;
	}

	private void notifyChanged() {
		if (onChange != null) {
			onChange.accept(value);
		}
	}

	/**
	 * Writes the value of the setting into a configuration document.
	 *
	 * @return the serialised value
	 */
	public abstract JsonElement write();

	/**
	 * Restores the value of the setting from a configuration document.
	 *
	 * @param element the serialised value, may be {@code null}
	 */
	public abstract void read(JsonElement element);
}
