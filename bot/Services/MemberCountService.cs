using Discord;
using Discord.Rest;

namespace CrossMod.Bot.Services;

/// <summary>
/// Keeps the name of the member counter channel up to date.
/// </summary>
public sealed class MemberCountService {

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
	public MemberCountService(Configuration.BotOptions options, DiscordRestClient client) {
		_options = options;
		_client = client;
	}

	/**
	 * Rewrites the name of the counter channel.
	 *
	 * @return whether the channel was updated
	 */
	public async Task<bool> UpdateAsync() {
		if (_options.MemberCountChannelId is null || _options.GuildId is null) {
			return false;
		}

		IGuild? guild = await _client.GetGuildAsync(_options.GuildId.Value, true);

		if (guild is null) {
			return false;
		}

		RestChannel? channel = await _client.GetChannelAsync(_options.MemberCountChannelId.Value);
		ITextChannel? text = channel as ITextChannel;

		if (text is null) {
			return false;
		}

		int members = guild.ApproximateMemberCount ?? 0;

		// Discord allows only small letters, numbers, dashes and underscores in
		// the name of a channel.
		string name = $"members-{members}";

		if (text.Name == name) {
			return true;
		}

		await text.ModifyAsync(properties => properties.Name = name);
		return true;
	}
}
