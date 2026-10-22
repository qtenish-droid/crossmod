package io.github.crossmod.client.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.crossmod.CrossMod;
import io.github.crossmod.config.ConfigManager;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of every module known to CrossMod.
 *
 * <p>The manager is the single place that knows about all modules. It owns the
 * life cycle, the key bindings and the persistence of the modules, so adding a
 * new module is a two line change: implement the module and register it
 * here.</p>
 */
public final class ModuleManager {

	private static final String CONFIG_SECTION = "modules";

	private final List<Module> modules = new ArrayList<>();
	private final Map<ModuleCategory, List<Module>> byCategory = new LinkedHashMap<>();

	/**
	 * Registers every built in module and binds their key bindings.
	 *
	 * <p>The registry deliberately starts empty. A module only becomes part of
	 * the client when it is added here, which keeps the shipped feature set
	 * explicit and easy to audit.</p>
	 */
	public void registerBuiltIns() {
		// register(new ExampleModule());
	}

	/**
	 * Registers a module and its key binding.
	 *
	 * @param module the module to register
	 */
	public void register(Module module) {
		if (module == null) {
			return;
		}

		modules.add(module);
		byCategory.computeIfAbsent(module.category(), category -> new ArrayList<>()).add(module);

		KeyMappingHelper.registerKeyMapping(module.keyBinding());
	}

	/**
	 * Returns every registered module.
	 *
	 * @return the immutable list of modules
	 */
	public List<Module> modules() {
		return Collections.unmodifiableList(modules);
	}

	/**
	 * Returns the modules of a single category.
	 *
	 * @param category the category
	 * @return the modules of the category
	 */
	public List<Module> modules(ModuleCategory category) {
		return Collections.unmodifiableList(byCategory.getOrDefault(category, List.of()));
	}

	/**
	 * Returns the module with the given identifier.
	 *
	 * @param id the identifier
	 * @return the module, or {@code null} when it does not exist
	 */
	public Module module(String id) {
		for (Module module : modules) {
			if (module.id().equals(id)) {
				return module;
			}
		}

		return null;
	}

	/**
	 * Ticks every enabled module and reacts to key binding presses.
	 */
	public void tick() {
		for (Module module : modules) {
			while (module.keyBinding().consumeClick()) {
				module.toggle();
				save();
			}

			if (module.isEnabled()) {
				module.onTick();
			}
		}
	}

	/**
	 * Writes the state of every module into the configuration.
	 */
	public void save() {
		ConfigManager config = CrossMod.config();
		JsonObject section = config.section(CONFIG_SECTION);

		for (Module module : modules) {
			section.add(module.id(), module.write());
		}

		config.save();
	}

	/**
	 * Restores the state of every module from the configuration.
	 */
	public void load() {
		ConfigManager config = CrossMod.config();
		JsonElement section = config.section(CONFIG_SECTION);

		if (!section.isJsonObject()) {
			return;
		}

		JsonObject values = section.getAsJsonObject();

		for (Module module : modules) {
			JsonElement stored = values.get(module.id());

			if (stored != null && stored.isJsonObject()) {
				module.read(stored.getAsJsonObject());
			}
		}
	}
}
