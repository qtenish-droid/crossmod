using Discord;
using Discord.Rest;

namespace CrossMod.Bot.Services;

/// <summary>
/// Builds and sends the announcements and the release posts.
///
/// <para>Everything is read over the REST interface of Discord, not out of the
/// cache of the gateway. The cache is filled by the events of the gateway and
/// is not always complete, the REST interface always has the truth.</para>
/// </summary>
public sealed class AnnouncementService {

	/** The accent colour of CrossMod. */
	public static readonly Color Accent = new(0x6FCBFF);

	/** The colour of a release post. */
	public static readonly Color Release = new(0xA6DFFF);

	/** The options of the bot. */
	private readonly Configuration.BotOptions _options;

	/** The client of the bot. */
	private readonly DiscordRestClient _client;

	/**
	 * Creates the service.
	 *
	 * @param options the options of the bot
	 * @param client  the REST client of the bot
	 */
	public AnnouncementService(Configuration.BotOptions options, DiscordRestClient client) {
		_options = options;
		_client = client;
	}

	/**
	 * Posts a release.
	 *
	 * @param version   the version of the release
	 * @param changelog the changelog of the release
	 * @param url       the address of the release, may be empty
	 * @param title     the title of the post, may be empty
	 * @return whether the post was sent
	 */
	public async Task<bool> PostReleaseAsync(string version, string changelog, string? url, string? title) {
		EmbedBuilder embed = new EmbedBuilder()
				.WithTitle(string.IsNullOrWhiteSpace(title) ? $"CrossMod {version}" : title)
				.WithDescription(string.IsNullOrWhiteSpace(changelog) ? "A new version of CrossMod is out." : changelog)
				.WithColor(Release)
				.WithAuthor("CrossMod")
				.WithFooter($"Version {version}")
				.WithTimestamp(DateTimeOffset.UtcNow);

		if (!string.IsNullOrWhiteSpace(url)) {
			embed.WithUrl(url);
		}

		return await SendAsync(embed.Build());
	}

	/**
	 * Posts an announcement.
	 *
	 * @param title the title of the post
	 * @param body  the body of the post
	 * @return whether the post was sent
	 */
	public async Task<bool> PostAnnouncementAsync(string title, string body) {
		EmbedBuilder embed = new EmbedBuilder()
				.WithTitle(title)
				.WithDescription(body)
				.WithColor(Accent)
				.WithAuthor("CrossMod")
				.WithTimestamp(DateTimeOffset.UtcNow);

		return await SendAsync(embed.Build());
	}

	/**
	 * Sends an embed into the announcement channel.
	 *
	 * @param embed the embed to send
	 * @return whether the embed was sent
	 */
	private async Task<bool> SendAsync(Embed embed) {
		IMessageChannel? channel = await ResolveAnnouncementChannel();

		if (channel is null) {
			Console.Error.WriteLine("No announcement channel is configured, the post was dropped.");
			return false;
		}

		await channel.SendMessageAsync(embed: embed);
		return true;
	}

	/**
	 * Returns the channel that receives the announcements.
	 *
	 * <p>A text channel and an announcement channel are both fine, the bot
	 * writes into whichever is configured.</p>
	 *
	 * @return the channel, or {@code null}
	 */
	public async Task<IMessageChannel?> ResolveAnnouncementChannel() {
		if (_options.AnnouncementChannelId is not null) {
			RestChannel? channel = await _client.GetChannelAsync(_options.AnnouncementChannelId.Value);

			if (channel is IMessageChannel message) {
				return message;
			}
		}

		if (_options.GuildId is null) {
			return null;
		}

		IGuild? guild = await _client.GetGuildAsync(_options.GuildId.Value);

		if (guild is null) {
			return null;
		}

		IEnumerable<IGuildChannel> channels = await guild.GetChannelsAsync();

		return channels.OfType<IMessageChannel>().FirstOrDefault();
	}
}
