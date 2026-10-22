using CrossMod.Bot.Data;
using CrossMod.Bot.Services;
using Discord;
using Discord.Interactions;

namespace CrossMod.Bot.Modules;

/// <summary>
/// The commands that manage the linked members.
/// </summary>
[Group("verify", "Manage the linked members")]
public sealed class VerifyCommands : CrossModModule {

	/**
	 * Links a member by hand.
	 *
	 * @param user      the member, as an id or a mention
	 * @param minecraft the name of the Minecraft account
	 * @return the task of the command
	 */
	[SlashCommand("add", "Link a member to a Minecraft account by hand")]
	public async Task AddAsync(string user, string minecraft) {
		ulong? guildId = GuildId;

		if (guildId is null) {
			await RespondAsync("This command only works in a server.");
			return;
		}

		ulong? userId = ParseId(user);

		if (userId is null) {
			await RespondAsync("That is not a member. Use the id of the member.");
			return;
		}

		IGuildUser? member = await BotServices.Verification.FindAsync(guildId.Value, userId.Value);

		if (member is null) {
			await RespondAsync("That member is not in this server.");
			return;
		}

		MinecraftProfile? profile = await BotServices.Mojang.ResolveAsync(minecraft);

		if (profile is null) {
			await RespondAsync("There is no Minecraft account named `" + minecraft + "`.");
			return;
		}

		await BotServices.Links.LinkManuallyAsync(member.Id, member.Username, profile.Uuid, profile.Name);
		await BotServices.Verification.GrantAsync(guildId.Value, member.Id);

		await RespondAsync(member.Username + " is now linked to `" + profile.Name + "`.");
	}

	/**
	 * Removes the link of a member.
	 *
	 * @param user the member, as an id or a mention
	 * @return the task of the command
	 */
	[SlashCommand("remove", "Remove the link of a member")]
	public async Task RemoveAsync(string user) {
		ulong? guildId = GuildId;

		if (guildId is null) {
			await RespondAsync("This command only works in a server.");
			return;
		}

		ulong? userId = ParseId(user);

		if (userId is null) {
			await RespondAsync("That is not a member. Use the id of the member.");
			return;
		}

		bool removed = await BotServices.Links.UnlinkAsync(userId.Value);
		await BotServices.Verification.RevokeAsync(guildId.Value, userId.Value);

		await RespondAsync(removed
				? "The link of the member was removed."
				: "That member was not linked.");
	}

	/**
	 * Lists the links of the server.
	 *
	 * @return the task of the command
	 */
	[SlashCommand("list", "List the linked members")]
	public async Task ListAsync() {
		List<LinkRecord> links = BotServices.Links.Links();

		if (links.Count == 0) {
			await RespondAsync("Nobody is linked yet.");
			return;
		}

		EmbedBuilder embed = new EmbedBuilder()
				.WithTitle("Linked members")
				.WithColor(AnnouncementService.Accent)
				.WithFooter(links.Count + " links");

		foreach (LinkRecord link in links.Take(25)) {
			embed.AddField(link.MinecraftName, link.DiscordUsername, true);
		}

		await RespondAsync(embed.Build());
	}
}
