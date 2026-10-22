using Microsoft.Extensions.Configuration;

namespace CrossMod.Bot.Configuration;

/// <summary>
/// Reads the {@link BotOptions} out of the configuration.
/// </summary>
public static class BotConfig {

	/**
	 * Reads the options.
	 *
	 * <p>The settings of the file {@code local.env} win over the environment,
	 * so the token can be kept on the machine. The environment wins over the
	 * appsettings file, because that is what the free hosts inject.</p>
	 *
	 * @param configuration the configuration of the host
	 * @return the options, never {@code null}
	 */
	public static BotOptions Load(IConfiguration configuration) {
		Dictionary<string, string> local = LocalEnv.Load();

		string? First(params string[] keys) {
			foreach (string key in keys) {
				if (local.TryGetValue(key, out string? value) && !string.IsNullOrWhiteSpace(value)) {
					return value.Trim();
				}
			}

			foreach (string key in keys) {
				string? value = configuration[key];

				if (!string.IsNullOrWhiteSpace(value)) {
					return value.Trim();
				}
			}

			return null;
		}

		ulong? Number(params string[] keys) {
			string? value = First(keys);
			return ulong.TryParse(value, out ulong number) ? number : null;
		}

		int Integer(int fallback, params string[] keys) {
			string? value = First(keys);
			return int.TryParse(value, out int number) ? number : fallback;
		}

		return new BotOptions {
			DiscordToken = First("CROSSMOD_DISCORD_TOKEN", "DISCORD_TOKEN", "CrossMod:DiscordToken") ?? string.Empty,
			GuildId = Number("CROSSMOD_GUILD_ID", "CrossMod:GuildId"),
			ApiKey = First("CROSSMOD_API_KEY", "CrossMod:ApiKey") ?? string.Empty,
			GitHubWebhookSecret = First("CROSSMOD_GITHUB_SECRET", "CrossMod:GitHubWebhookSecret"),
			WelcomeChannelId = Number("CROSSMOD_WELCOME_CHANNEL_ID", "CrossMod:WelcomeChannelId"),
			MemberCountChannelId = Number("CROSSMOD_MEMBER_COUNT_CHANNEL_ID", "CrossMod:MemberCountChannelId"),
			AnnouncementChannelId = Number("CROSSMOD_ANNOUNCEMENT_CHANNEL_ID", "CrossMod:AnnouncementChannelId"),
			VerifiedRoleId = Number("CROSSMOD_VERIFIED_ROLE_ID", "CrossMod:VerifiedRoleId"),
			StaffRoleId = Number("CROSSMOD_STAFF_ROLE_ID", "CrossMod:StaffRoleId"),
			DataPath = First("CROSSMOD_DATA_PATH", "CrossMod:DataPath") ?? "data/state.json",
			HttpPort = Integer(8080, "CROSSMOD_PORT", "PORT", "CrossMod:HttpPort"),
			PublicUrl = First("CROSSMOD_PUBLIC_URL", "CrossMod:PublicUrl") ?? string.Empty,
			Offline = string.Equals(First("CROSSMOD_OFFLINE", "CrossMod:Offline"), "true", StringComparison.OrdinalIgnoreCase),
			MembersIntent = !string.Equals(First("CROSSMOD_MEMBERS_INTENT", "CrossMod:MembersIntent"), "false", StringComparison.OrdinalIgnoreCase)
		};
	}

	/**
	 * Fails fast when a value the bot cannot work without is missing.
	 *
	 * @param options the options to check
	 */
	public static void Validate(BotOptions options) {
		List<string> missing = new();

		if (string.IsNullOrWhiteSpace(options.DiscordToken)) {
			missing.Add("CROSSMOD_DISCORD_TOKEN");
		}

		if (string.IsNullOrWhiteSpace(options.ApiKey)) {
			missing.Add("CROSSMOD_API_KEY");
		}

		if (missing.Count > 0) {
			throw new InvalidOperationException(
					"Missing required environment variables: " + string.Join(", ", missing)
					+ ". Put them into the environment or into bot/" + LocalEnv.FILE_NAME + ", see bot/README.md.");
		}
	}
}
