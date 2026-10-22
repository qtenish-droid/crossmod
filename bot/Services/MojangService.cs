using System.Text.Json;

namespace CrossMod.Bot.Services;

/// <summary>
/// A Minecraft account, as the official API reports it.
/// </summary>
public sealed class MinecraftProfile {

	/** The uuid of the account, without dashes. */
	public string Uuid { get; init; } = string.Empty;

	/** The name of the account. */
	public string Name { get; init; } = string.Empty;
}

/// <summary>
/// Resolves Minecraft names against the official Mojang API.
///
/// <para>The lookup makes sure that a name is a real Minecraft account and that
/// the uuid belongs to that name. The name is the only part the player can
/// spoof freely, so it is the part that is checked here.</para>
/// </summary>
public sealed class MojangService {

	/** The client that talks to the Mojang API. */
	private readonly HttpClient _http = new() {
		Timeout = TimeSpan.FromSeconds(10)
	};

	/**
	 * Creates the service.
	 */
	public MojangService() {
		_http.DefaultRequestHeaders.Add("User-Agent", "CrossMod-Bot/0.1 (https://github.com/crossmod)");
	}

	/**
	 * Resolves a name into the account behind it.
	 *
	 * @param name the name of the Minecraft account
	 * @return the account, or {@code null} when the name is unknown
	 */
	public async Task<MinecraftProfile?> ResolveAsync(string name) {
		string cleaned = name.Trim();

		if (cleaned.Length is < 3 or > 16) {
			return null;
		}

		try {
			using HttpResponseMessage response = await _http
					.GetAsync($"https://api.mojang.com/users/profiles/minecraft/{Uri.EscapeDataString(cleaned)}")
					.ConfigureAwait(false);

			if (!response.IsSuccessStatusCode) {
				return null;
			}

			string body = await response.Content.ReadAsStringAsync().ConfigureAwait(false);

			using JsonDocument document = JsonDocument.Parse(body);

			if (!document.RootElement.TryGetProperty("id", out JsonElement id)) {
				return null;
			}

			string uuid = id.GetString() ?? string.Empty;

			if (uuid.Length != 32) {
				return null;
			}

			string canonical = document.RootElement.TryGetProperty("name", out JsonElement value)
					? value.GetString() ?? cleaned
					: cleaned;

			return new MinecraftProfile {
				Uuid = uuid,
				Name = canonical
			};
		} catch (Exception exception) {
			Console.Error.WriteLine($"The Mojang lookup for '{cleaned}' failed: {exception.Message}");
			return null;
		}
	}

	/**
	 * Checks whether a string is a uuid in the dashed form.
	 *
	 * @param value the value to check
	 * @return whether the value is a uuid
	 */
	public static bool IsUuid(string value) {
		return Guid.TryParse(value, out _);
	}

	/**
	 * Removes the dashes of a uuid.
	 *
	 * @param value the uuid
	 * @return the uuid without dashes
	 */
	public static string NormalizeUuid(string value) {
		return value.Replace("-", string.Empty).Trim().ToLowerInvariant();
	}
}
