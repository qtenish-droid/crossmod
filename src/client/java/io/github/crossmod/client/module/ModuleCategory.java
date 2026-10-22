package io.github.crossmod.client.module;

import net.minecraft.network.chat.Component;

/**
 * The categories a module can belong to.
 *
 * <p>The order of the constants is the order in which the categories are
 * rendered as columns inside the interface, so the declaration order is part of
 * the user interface and must stay stable.</p>
 */
public enum ModuleCategory {

	/** Combat oriented modules. */
	PVP("PvP", "pvp"),

	/** Modules that improve the performance of the game. */
	PERF("Perf", "perf"),

	/** Modules that improve the quality of life. */
	UTIL("Util", "util");

	private final String displayName;
	private final String id;

	ModuleCategory(String displayName, String id) {
		this.displayName = displayName;
		this.id = id;
	}

	/**
	 * Returns the human readable name of the category.
	 *
	 * @return the display name
	 */
	public String displayName() {
		return displayName;
	}

	/**
	 * Returns the stable identifier of the category, used for configuration.
	 *
	 * @return the identifier
	 */
	public String id() {
		return id;
	}

	/**
	 * Returns the translated name of the category.
	 *
	 * @return the name as a component
	 */
	public Component title() {
		return Component.translatable("crossmod.category." + id);
	}
}
