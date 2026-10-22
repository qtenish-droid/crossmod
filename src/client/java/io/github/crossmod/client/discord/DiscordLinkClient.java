package io.github.crossmod.client.discord;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.crossmod.CrossMod;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * The client that talks to the CrossMod bot.
 *
 * <p>The bot owns the link between a Discord account and a Minecraft account.
 * The client only hands the code of the player over to the bot, the bot
 * checks it, resolves the Minecraft account and grants the verified role.</p>
 *
 * <p>The address of the bot and the shared secret are read from the
 * configuration file of the mod, in the section {@code discord}:</p>
 *
 * <pre>
 * {
 *   "discord": {
 *     "api_url": "https://crossmod-bot.example.com",
 *     "api_key": "the shared secret"
 *   }
 * }
 * </pre>
 */
public final class DiscordLinkClient {

	/** The section of the configuration that holds the settings of the bot. */
	public static final String SECTION = "discord";

	/** The key of the address of the bot. */
	public static final String KEY_URL = "api_url";

	/** The key of the shared secret of the bot. */
	public static final String KEY_TOKEN = "api_key";

	/** The client that performs the requests. */
	private final HttpClient http = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();

	/**
	 * The answer of the bot.
	 *
	 * @param success whether the account was linked
	 * @param message the text that is shown to the player
	 */
	public record LinkResult(boolean success, String message) {
	}

	/**
	 * Returns the address of the bot.
	 *
	 * @return the address, empty when the bot is not configured
	 */
	public String apiUrl() {
		return CrossMod.config().getString(SECTION, KEY_URL, "").trim();
	}

	/**
	 * Returns the shared secret of the bot.
	 *
	 * @return the secret, empty when the bot is not configured
	 */
	public String apiToken() {
		return CrossMod.config().getString(SECTION, KEY_TOKEN, "").trim();
	}

	/**
	 * Returns whether the bot is configured.
	 *
	 * @return whether the client can talk to the bot
	 */
	public boolean isConfigured() {
		return !apiUrl().isEmpty() && !apiToken().isEmpty();
	}

	/**
	 * Links the account of the player with the Discord account that owns the
	 * code.
	 *
	 * @param code          the code the player got from the bot
	 * @param minecraftName the name of the Minecraft account
	 * @return the answer of the bot
	 */
	public CompletableFuture<LinkResult> link(String code, String minecraftName) {
		JsonObject body = new JsonObject();
		body.addProperty("code", code);
		body.addProperty("minecraftName", minecraftName);

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(apiUrl() + "/api/link"))
				.timeout(Duration.ofSeconds(15))
				.header("Content-Type", "application/json")
				.header("X-Api-Key", apiToken())
				.POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
				.build();

		return http.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
				.thenApply(this::read);
	}

	/**
	 * Reads the answer of the bot.
	 *
	 * @param response the response of the bot
	 * @return the result of the link
	 */
	private LinkResult read(HttpResponse<String> response) {
		String body = response.body();

		try {
			JsonObject json = JsonParser.parseString(body).getAsJsonObject();

			if (response.statusCode() == 200 && json.has("ok") && json.get("ok").getAsBoolean()) {
				String name = json.has("minecraftName") ? json.get("minecraftName").getAsString() : "your account";
				return new LinkResult(true, "Your Discord account is now linked to " + name + ".");
			}

			if (json.has("error")) {
				return new LinkResult(false, json.get("error").getAsString());
			}
		} catch (RuntimeException exception) {
			// The answer is not the expected one, the status is reported below.
			CrossMod.LOGGER.warn("[CrossMod] The bot answered with an unexpected body.", exception);
		}

		return new LinkResult(false, "The bot answered with " + response.statusCode() + ".");
	}
}
