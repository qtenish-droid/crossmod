using CrossMod.Bot.Configuration;
using CrossMod.Bot.Data;
using CrossMod.Bot.Services;
using Discord.Rest;
using Discord.WebSocket;

namespace CrossMod.Bot;

/// <summary>
/// The services of the bot, reachable from everywhere.
///
/// <para>The commands of the bot are created by the interaction library and
/// not by the container of the host, so the services are handed over here once
/// at the start. Everything in here is a singleton and lives as long as the
/// bot.</para>
/// </summary>
public static class BotServices {

	/** The options of the bot. */
	public static BotOptions Options { get; private set; } = new();

	/** The client of the bot. */
	public static DiscordSocketClient Client { get; private set; } = null!;

	/** The REST client of the bot. */
	public static DiscordRestClient Rest { get; private set; } = null!;

	/** The state of the bot. */
	public static StateStore Store { get; private set; } = null!;

	/** The links between the Discord and the Minecraft accounts. */
	public static LinkService Links { get; private set; } = null!;

	/** The lookup of the Minecraft names. */
	public static MojangService Mojang { get; private set; } = null!;

	/** The service that grants the verified role. */
	public static VerificationService Verification { get; private set; } = null!;

	/** The service that posts the announcements. */
	public static AnnouncementService Announcements { get; private set; } = null!;

	/** The service that handles the reaction roles. */
	public static ReactionRoleService ReactionRoles { get; private set; } = null!;

	/** The service that welcomes new members. */
	public static WelcomeService Welcome { get; private set; } = null!;

	/**
	 * Hands the services over to the commands.
	 *
	 * @param options       the options of the bot
	 * @param client        the client of the bot
	 * @param rest          the REST client of the bot
	 * @param store         the state of the bot
	 * @param links         the links between the accounts
	 * @param mojang        the lookup of the Minecraft names
	 * @param verification  the service that grants the verified role
	 * @param announcements the service that posts the announcements
	 * @param reactionRoles the service that handles the reaction roles
	 * @param welcome       the service that welcomes new members
	 */
	public static void Initialize(
			BotOptions options,
			DiscordSocketClient client,
			DiscordRestClient rest,
			StateStore store,
			LinkService links,
			MojangService mojang,
			VerificationService verification,
			AnnouncementService announcements,
			ReactionRoleService reactionRoles,
			WelcomeService welcome) {
		Options = options;
		Client = client;
		Rest = rest;
		Store = store;
		Links = links;
		Mojang = mojang;
		Verification = verification;
		Announcements = announcements;
		ReactionRoles = reactionRoles;
		Welcome = welcome;
	}
}
