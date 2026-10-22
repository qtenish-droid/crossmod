package io.github.crossmod;

import io.github.crossmod.config.ConfigManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entry point of CrossMod.
 *
 * <p>Only environment agnostic services are bootstrapped here - the configuration
 * store is the single piece of shared state between the common and the client
 * source set. Everything that touches Minecraft classes lives in
 * {@code io.github.crossmod.client}.</p>
 */
public final class CrossMod implements ModInitializer {

	/** Shared logger for the whole mod. */
	public static final Logger LOGGER = LoggerFactory.getLogger("CrossMod");

	/** Identifier of the mod, also used as the configuration file name. */
	public static final String MOD_ID = "crossmod";

	private static ConfigManager configManager;

	@Override
	public void onInitialize() {
		configManager = new ConfigManager(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json"));
		configManager.load();

		LOGGER.info("CrossMod initialised (config: {})", configManager.getConfigPath());
	}

	/**
	 * Returns the shared configuration store.
	 *
	 * @return the configuration store, never {@code null} after initialization
	 */
	public static ConfigManager config() {
		if (configManager == null) {
			// Defensive fallback: the initializer always runs first, but the store
			// is still required for the configuration screen to be usable.
			configManager = new ConfigManager(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json"));
			configManager.load();
		}
		return configManager;
	}
}
