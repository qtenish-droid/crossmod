using CrossMod.Bot.Data;
using Discord;
using Discord.Rest;

namespace CrossMod.Bot.Services;

/// <summary>
/// Handles the reactions that grant a role.
/// </summary>
public sealed class ReactionRoleService {

	/** The state of the bot. */
	private readonly StateStore _store;

	/** The client of the bot. */
	private readonly DiscordRestClient _client;

	/**
	 * Creates the service.
	 *
	 * @param store the state of the bot
	 * @param client the REST client of the bot
	 */
	public ReactionRoleService(StateStore store, DiscordRestClient client) {
		_store = store;
		_client = client;
	}

	/**
	 * Adds a rule.
	 *
	 * @param rule the rule to add
	 * @return whether the rule was added
	 */
	public async Task<bool> AddAsync(ReactionRoleRule rule) {
		return await _store.UpdateAsync(state => {
			state.ReactionRoles.RemoveAll(existing =>
					existing.MessageId == rule.MessageId && SameEmoji(existing.Emoji, rule.Emoji));
			state.ReactionRoles.Add(rule);
			return true;
		});
	}

	/**
	 * Removes a rule.
	 *
	 * @param messageId the id of the message
	 * @param emoji     the emoji of the rule
	 * @return whether a rule was removed
	 */
	public async Task<bool> RemoveAsync(ulong messageId, string emoji) {
		return await _store.UpdateAsync(state => {
			int removed = state.ReactionRoles.RemoveAll(existing =>
					existing.MessageId == messageId && SameEmoji(existing.Emoji, emoji));
			return removed > 0;
		});
	}

	/**
	 * Returns all the rules.
	 *
	 * @return the rules
	 */
	public List<ReactionRoleRule> Rules() {
		return _store.State.ReactionRoles.ToList();
	}

	/**
	 * Grants the roles of a message when a reaction is added.
	 *
	 * @param messageId the id of the message
	 * @param channelId the id of the channel of the message
	 * @param emote     the emoji that was added
	 * @param userId    the id of the member that reacted
	 * @return the names of the granted roles
	 */
	public async Task<List<string>> GrantAsync(ulong messageId, ulong channelId, IEmote emote, ulong userId) {
		List<string> granted = new();

		foreach (ReactionRoleRule rule in _store.State.ReactionRoles.Where(rule =>
				rule.MessageId == messageId && SameEmoji(rule.Emoji, emote))) {
			string? name = await ApplyAsync(channelId, rule.RoleId, userId, true);

			if (name is not null) {
				granted.Add(name);
			}
		}

		return granted;
	}

	/**
	 * Takes the roles of a message away when a reaction is removed.
	 *
	 * @param messageId the id of the message
	 * @param channelId the id of the channel of the message
	 * @param emote     the emoji that was removed
	 * @param userId    the id of the member that reacted
	 * @return the names of the removed roles
	 */
	public async Task<List<string>> RevokeAsync(ulong messageId, ulong channelId, IEmote emote, ulong userId) {
		List<string> revoked = new();

		foreach (ReactionRoleRule rule in _store.State.ReactionRoles.Where(rule =>
				rule.MessageId == messageId && SameEmoji(rule.Emoji, emote))) {
			string? name = await ApplyAsync(channelId, rule.RoleId, userId, false);

			if (name is not null) {
				revoked.Add(name);
			}
		}

		return revoked;
	}

	/**
	 * Grants or takes a role away.
	 *
	 * @param channelId the id of the channel the message is in
	 * @param roleId    the id of the role
	 * @param userId    the id of the member
	 * @param grant     whether the role is granted or removed
	 * @return the name of the role, or {@code null} when nothing happened
	 */
	private async Task<string?> ApplyAsync(ulong channelId, ulong roleId, ulong userId, bool grant) {
		IGuild? guild = await FindGuildAsync(channelId);

		if (guild is null) {
			return null;
		}

		IGuildUser? member = await guild.GetUserAsync(userId);
		IRole? role = guild.GetRole(roleId);

		if (member is null || role is null) {
			return null;
		}

		bool hasRole = member.RoleIds.Contains(roleId);

		if (grant && !hasRole) {
			await member.AddRoleAsync(role);
		} else if (!grant && hasRole) {
			await member.RemoveRoleAsync(role);
		} else {
			return null;
		}

		return role.Name;
	}

	/**
	 * Returns the guild of a channel.
	 *
	 * @param channelId the id of the channel
	 * @return the guild, or {@code null}
	 */
	private async Task<IGuild?> FindGuildAsync(ulong channelId) {
		RestChannel? channel = await _client.GetChannelAsync(channelId);
		IGuildChannel? guildChannel = channel as IGuildChannel;

		if (guildChannel is null) {
			return null;
		}

		return await _client.GetGuildAsync(guildChannel.GuildId);
	}

	/**
	 * Compares a stored emoji with the one of a reaction.
	 *
	 * <p>Custom emojis are compared by their full form and by their name, the
	 * normal ones by their name. A rule that was written as a name therefore
	 * keeps working when the reaction is read from the gateway.</p>
	 *
	 * @param stored the emoji of the rule
	 * @param emote  the emoji of the reaction
	 * @return whether both describe the same emoji
	 */
	public static bool SameEmoji(string stored, IEmote emote) {
		string left = stored.Trim();
		string right = emote.ToString() ?? string.Empty;

		if (left.Equals(right, StringComparison.OrdinalIgnoreCase)) {
			return true;
		}

		string name = Name(left);
		string other = emote.Name;

		return name.Length > 0 && name.Equals(other, StringComparison.OrdinalIgnoreCase);
	}

	/**
	 * Compares two stored emojis.
	 *
	 * @param left  the first emoji
	 * @param right the second emoji
	 * @return whether both describe the same emoji
	 */
	public static bool SameEmoji(string left, string right) {
		string first = left.Trim();
		string second = right.Trim();

		if (first.Equals(second, StringComparison.OrdinalIgnoreCase)) {
			return true;
		}

		string name = Name(first);
		string other = Name(second);

		return name.Length > 0 && name.Equals(other, StringComparison.OrdinalIgnoreCase);
	}

	/**
	 * Returns the name of an emoji.
	 *
	 * @param emoji the emoji
	 * @return the name
	 */
	private static string Name(string emoji) {
		if (emoji.StartsWith("<")) {
			int end = emoji.IndexOf('>');
			string inner = end > 0 ? emoji[1..end] : emoji[1..];
			int colon = inner.IndexOf(':');
			return colon > 0 ? inner[(colon + 1)..] : inner;
		}

		if (emoji.StartsWith(":") && emoji.EndsWith(":") && emoji.Length > 2) {
			return emoji[1..^1];
		}

		return emoji;
	}
}
