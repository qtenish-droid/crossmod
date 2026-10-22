package io.github.crossmod.client.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.crossmod.client.keybind.KeyBinds;
import io.github.crossmod.client.setting.Setting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class of every CrossMod feature.
 *
 * <p>A module is a self contained unit of behaviour. It owns a list of
 * {@link Setting settings}, an optional key binding and a simple life cycle
 * ({@link #onEnable()}, {@link #onDisable()}, {@link #onTick()}). The module
 * manager is responsible for toggling, ticking and persisting modules; a
 * module never has to know about the configuration format or the interface.</p>
 */
public abstract class Module {

	private final String id;
	private final String name;
	private final String description;
	private final ModuleCategory category;
	private final List<Setting<?>> settings = new ArrayList<>();
	private final KeyMapping keyBinding;

	private boolean enabled;

	protected Module(String id, String name, String description, ModuleCategory category) {
		this(id, name, description, category, InputConstants.UNKNOWN.getValue());
	}

	protected Module(String id, String name, String description, ModuleCategory category, int defaultKeyCode) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.category = category;
		this.enabled = false;
		this.keyBinding = new KeyMapping(
				"key.crossmod.module." + id,
				InputConstants.Type.KEYBOARD,
				defaultKeyCode,
				KeyBinds.CATEGORY
		);
	}

	/**
	 * Returns the stable identifier of the module.
	 *
	 * @return the identifier
	 */
	public String id() {
		return id;
	}

	/**
	 * Returns the human readable name of the module.
	 *
	 * @return the name
	 */
	public String name() {
		return name;
	}

	/**
	 * Returns the description of the module, shown as a tooltip.
	 *
	 * @return the description
	 */
	public String description() {
		return description;
	}

	/**
	 * Returns the translated name of the module.
	 *
	 * @return the name as a component
	 */
	public Component title() {
		return Component.translatable("crossmod.module." + id);
	}

	/**
	 * Returns the category the module belongs to.
	 *
	 * @return the category
	 */
	public ModuleCategory category() {
		return category;
	}

	/**
	 * Returns the settings of the module.
	 *
	 * @return the immutable list of settings
	 */
	public List<Setting<?>> settings() {
		return Collections.unmodifiableList(settings);
	}

	/**
	 * Registers a setting on the module.
	 *
	 * @param <T>    the type of the setting
	 * @param setting the setting to add
	 * @return the added setting
	 */
	protected <T extends Setting<?>> T addSetting(T setting) {
		settings.add(setting);
		return setting;
	}

	/**
	 * Returns the key binding of the module.
	 *
	 * @return the key binding, never {@code null}
	 */
	public KeyMapping keyBinding() {
		return keyBinding;
	}

	/**
	 * Returns whether the module is currently enabled.
	 *
	 * @return {@code true} when the module is active
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Enables or disables the module.
	 *
	 * <p>The life cycle callbacks are invoked by the module manager, which makes
	 * sure that a module is never enabled twice.</p>
	 *
	 * @param enabled the new state
	 */
	public void setEnabled(boolean enabled) {
		if (this.enabled == enabled) {
			return;
		}

		this.enabled = enabled;

		if (enabled) {
			onEnable();
		} else {
			onDisable();
		}
	}

	/**
	 * Flips the state of the module.
	 */
	public void toggle() {
		setEnabled(!enabled);
	}

	/**
	 * Called when the module becomes active.
	 */
	public void onEnable() {
	}

	/**
	 * Called when the module is switched off.
	 */
	public void onDisable() {
	}

	/**
	 * Returns the font of the game.
	 *
	 * @return the font
	 */
	protected net.minecraft.client.gui.Font font() {
		return net.minecraft.client.Minecraft.getInstance().font;
	}

	/**
	 * Called every client tick while the module is active.
	 */
	public void onTick() {
	}

	/**
	 * Returns whether the module draws something on the heads up display.
	 *
	 * @return {@code true} when the module has a heads up display element
	 */
	public boolean hasHud() {
		return false;
	}

	/**
	 * Draws the heads up display element of the module.
	 *
	 * @param graphics     the graphics context
	 * @param deltaTracker the delta tracker of the current frame
	 */
	public void renderHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
	}

	/**
	 * Writes the state and the settings of the module into the configuration.
	 *
	 * @return the serialised module
	 */
	public JsonObject write() {
		JsonObject object = new JsonObject();
		object.addProperty("enabled", enabled);

		JsonObject settingValues = new JsonObject();

		for (Setting<?> setting : settings) {
			JsonElement value = setting.write();

			if (value != null) {
				settingValues.add(setting.id(), value);
			}
		}

		object.add("settings", settingValues);
		return object;
	}

	/**
	 * Restores the state and the settings of the module from the configuration.
	 *
	 * @param object the serialised module, may be {@code null}
	 */
	public void read(JsonObject object) {
		if (object == null) {
			return;
		}

		if (object.has("settings") && object.get("settings").isJsonObject()) {
			JsonObject settingValues = object.getAsJsonObject("settings");

			for (Setting<?> setting : settings) {
				if (settingValues.has(setting.id())) {
					setting.read(settingValues.get(setting.id()));
				}
			}
		}

		if (object.has("enabled") && object.get("enabled").isJsonPrimitive()) {
			enabled = object.get("enabled").getAsBoolean();
		}
	}
}
