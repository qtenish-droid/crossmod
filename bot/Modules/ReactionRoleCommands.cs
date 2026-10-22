using CrossMod.Bot.Data;
using CrossMod.Bot.Services;
using Discord;
using Discord.Interactions;

namespace CrossMod.Bot.Modules;

/// <summary>
/// The commands that manage the reaction roles.
/// </summary>
[Group("reactionrole", "Let a reaction grant a role")]
public sealed class ReactionRoleCommands : CrossModModule {

	/**
	 * Adds a rule.
	 *
	 * @param channel the channel of the message, as an id or a mention
	 * @param message the id of the message
	 * @param emoji   the emoji that grants the role
	 * @param role    the role that is granted, as an id or a mention
	 * @return the task of the command
	 */
	[SlashCommand("add", "Let a reaction grant a role")]
	[RequireStaff]
	public async Task AddAsync(string channel, string message, string emoji, string role) {
		ulong? channelId = ParseId(channel);
		ulong? messageId = ParseId(message);
		ulong? roleId = ParseId(role);

		if (channelId is null || messageId is null || roleId is null) {
			await RespondAsync("The channel, the message and the role need their id. Turn on the developer mode of Discord to copy them.");
			return;
		}

		IGuild? guild = GuildId is ulong id ? await BotServices.Rest.GetGuildAsync(id) : null;
		IRole? target = guild?.GetRole(roleId.Value);

		if (target is null) {
			await RespondAsync("There is no such role in this server.");
			return;
		}

		await BotServices.ReactionRoles.AddAsync(new ReactionRoleRule {
			MessageId = messageId.Value,
			ChannelId = channelId.Value,
			Emoji = emoji,
			RoleId = roleId.Value,
			Label = target.Name
		});

		await RespondAsync("Reacting with " + emoji + " on that message now grants `" + target.Name + "`.");
	}

	/**
	 * Removes a rule.
	 *
	 * @param message the id of the message
	 * @param emoji   the emoji of the rule
	 * @return the task of the command
	 */
	[SlashCommand("remove", "Stop a reaction from granting a role")]
	[RequireStaff]
	public async Task RemoveAsync(string message, string emoji) {
		ulong? messageId = ParseId(message);

		if (messageId is null) {
			await RespondAsync("That is not a message id.");
			return;
		}

		bool removed = await BotServices.ReactionRoles.RemoveAsync(messageId.Value, emoji);

		await RespondAsync(removed ? "The rule was removed." : "There was no such rule.");
	}

	/**
	 * Lists the rules.
	 *
	 * @return the task of the command
	 */
	[SlashCommand("list", "List the reaction roles")]
	[RequireStaff]
	public async Task ListAsync() {
		List<ReactionRoleRule> rules = BotServices.ReactionRoles.Rules();

		if (rules.Count == 0) {
			await RespondAsync("There are no reaction roles yet.");
			return;
		}

		EmbedBuilder embed = new EmbedBuilder()
				.WithTitle("Reaction roles")
				.WithColor(AnnouncementService.Accent);

		foreach (ReactionRoleRule rule in rules.Take(25)) {
			embed.AddField(rule.Emoji, rule.Label + " - message " + rule.MessageId, true);
		}

		await RespondAsync(embed.Build());
	}
}
