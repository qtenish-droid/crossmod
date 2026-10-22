package io.github.crossmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Small, dependency free JSON backed configuration store.
 *
 * <p>The store is intentionally generic: modules and GUI settings read and write
 * their own values into named sections. That keeps the persistence layer free
 * of any knowledge about the concrete features that use it.</p>
 */
public final class ConfigManager {

	private static final Logger LOGGER = LoggerFactory.getLogger("CrossMod");

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private final Path configPath;
	private final JsonObject root = new JsonObject();

	/**
	 * Creates a configuration store backed by the given file.
	 *
	 * @param configPath the file the configuration is read from and written to
	 */
	public ConfigManager(Path configPath) {
		this.configPath = configPath;
	}

	/**
	 * Returns the file this store is persisted to.
	 *
	 * @return the configuration file path
	 */
	public Path getConfigPath() {
		return configPath;
	}

	/**
	 * Reads the configuration file from disk. Missing or malformed files simply
	 * result in an empty configuration so the mod always starts.
	 */
	public void load() {
		root.asMap().clear();

		if (!Files.exists(configPath)) {
			return;
		}

		try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
			JsonElement parsed = JsonParser.parseReader(reader);

			if (parsed.isJsonObject()) {
				root.asMap().putAll(parsed.getAsJsonObject().asMap());
			}
		} catch (IOException | RuntimeException exception) {
			LOGGER.warn("[CrossMod] Could not read configuration, using defaults.", exception);
		}
	}

	/**
	 * Writes the configuration back to disk.
	 */
	public void save() {
		try {
			Path parent = configPath.getParent();

			if (parent != null) {
				Files.createDirectories(parent);
			}

			try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
				GSON.toJson(root, writer);
			}
		} catch (IOException exception) {
			LOGGER.warn("[CrossMod] Could not write configuration.", exception);
		}
	}

	/**
	 * Returns a mutable section of the configuration, creating it when missing.
	 *
	 * @param name the section name
	 * @return the section object
	 */
	public JsonObject section(String name) {
		JsonElement existing = root.get(name);

		if (existing != null && existing.isJsonObject()) {
			return existing.getAsJsonObject();
		}

		JsonObject created = new JsonObject();
		root.add(name, created);
		return created;
	}

	/**
	 * Returns a value from a section, falling back to a default.
	 *
	 * @param section    the section name
	 * @param key        the key inside the section
	 * @param fallback   the value returned when the key is missing
	 * @return the stored value or the fallback
	 */
	public String getString(String section, String key, String fallback) {
		JsonObject object = root.has(section) && root.get(section).isJsonObject() ? root.getAsJsonObject(section) : null;

		if (object == null) {
			return fallback;
		}

		JsonElement element = object.get(key);

		if (element != null && element.isJsonPrimitive()) {
			return element.getAsString();
		}

		return fallback;
	}

	/**
	 * Stores a string value in a section.
	 *
	 * @param section the section name
	 * @param key     the key inside the section
	 * @param value   the value to store
	 */
	public void setString(String section, String key, String value) {
		section(section).add(key, new JsonPrimitive(value));
	}

	/**
	 * Returns a boolean value from a section, falling back to a default.
	 *
	 * @param section  the section name
	 * @param key      the key inside the section
	 * @param fallback the value returned when the key is missing
	 * @return the stored value or the fallback
	 */
	public boolean getBoolean(String section, String key, boolean fallback) {
		JsonObject object = root.has(section) && root.get(section).isJsonObject() ? root.getAsJsonObject(section) : null;

		if (object == null) {
			return fallback;
		}

		JsonElement element = object.get(key);

		if (element != null && element.isJsonPrimitive()) {
			return element.getAsBoolean();
		}

		return fallback;
	}

	/**
	 * Stores a boolean value in a section.
	 *
	 * @param section the section name
	 * @param key     the key inside the section
	 * @param value   the value to store
	 */
	public void setBoolean(String section, String key, boolean value) {
		section(section).add(key, new JsonPrimitive(value));
	}
}
