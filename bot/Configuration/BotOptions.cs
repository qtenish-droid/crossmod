namespace CrossMod.Bot.Configuration;

/// <summary>
/// Everything the bot needs to know at runtime.
///
/// <para>Every value is read from the environment, because that is what the free
/// cloud hosts (Railway, Render, Fly.io, Koyeb) inject. The names are the
/// {@code CROSSMOD_} prefixed variants, the plain Discord ones
/// ({@code DISCORD_TOKEN}) are accepted as a fallback.</para>
/// </summary>
public sealed class BotOptions {

	/** The bot token from the Discord developer portal. */
	public string DiscordToken { get; set; } = string.Empty;

	/**
	 * The guild the slash commands are registered in.
	 *
	 * <p>Guild commands appear instantly, global commands take up to an hour to
	 * propagate. A development bot should always use a guild.</p>
	 */
	public ulong? GuildId { get; set; }

	/** The shared secret that protects the HTTP endpoints. */
	public string ApiKey { get; set; } = string.Empty;

	/** The secret of the GitHub webhook, if the GitHub endpoint is used. */
	public string? GitHubWebhookSecret { get; set; }

	/** The channel that welcomes new members. */
	public ulong? WelcomeChannelId { get; set; }

	/** The channel that shows the member count. */
	public ulong? MemberCountChannelId { get; set; }

	/** The channel that receives announcements and releases. */
	public ulong? AnnouncementChannelId { get; set; }

	/** The role that is granted to a linked member. */
	public ulong? VerifiedRoleId { get; set; }

	/** The role that unlocks the staff commands. */
	public ulong? StaffRoleId { get; set; }

	/** The file the state (links, reaction roles) is stored in. */
	public string DataPath { get; set; } = "data/state.json";

	/** The port the HTTP endpoint listens on. */
	public int HttpPort { get; set; } = 8080;

	/** The base address the mod and the CI pipeline talk to. */
	public string PublicUrl { get; set; } = string.Empty;

	/**
	 * Whether the bot starts without a connection to Discord.
	 *
	 * <p>Only useful while the interface of the bot is worked on: the HTTP
	 * endpoints are served, but no command is registered and no message is
	 * sent.</p>
	 */
	public bool Offline { get; set; }

	/**
	 * Whether the bot asks for the privileged members intent.
	 *
	 * <p>The intent is needed for the welcome messages and the member counter.
	 * Discord only hands it out when the server members intent is switched on
	 * in the developer portal, and it closes the connection when a privileged
	 * intent is missing.</p>
	 */
	public bool MembersIntent { get; set; } = true;
}
