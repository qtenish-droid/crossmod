using Discord;
using Discord.Rest;
using Discord.WebSocket;

namespace CrossMod.Bot.Services;

/// <summary>
/// Welcomes new members.
/// </summary>
public sealed class WelcomeService {

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
	public WelcomeService(Configuration.BotOptions options, DiscordRestClient client) {
		_options = options;
		_client = client;
	}

	/**
	 * Welcomes a member in the welcome channel.
	 *
	 * @param member the member that joined
	 * @return whether the message was sent
	 */
	public async Task<bool> WelcomeAsync(SocketGuildUser member) {
		if (_options.WelcomeChannelId is null) {
			return false;
		}

		RestChannel? channel = await _client.GetChannelAsync(_options.WelcomeChannelId.Value);
		IMessageChannel? message = channel as IMessageChannel;

		if (message is null) {
			return false;
		}

		EmbedBuilder embed = new EmbedBuilder()
				.WithTitle($"Welcome to {member.Guild.Name}")
				.WithDescription($"Welcome {member.Mention}, good to see you here.")
				.WithColor(AnnouncementService.Accent)
				.WithThumbnailUrl(member.GetAvatarUrl())
				.WithTimestamp(DateTimeOffset.UtcNow);

		await message.SendMessageAsync(embed: embed.Build());
		return true;
	}
}
