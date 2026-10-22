using CrossMod.Bot.Data;
using CrossMod.Bot.Services;
using Discord;
using Discord.Interactions;

namespace CrossMod.Bot.Modules;

/// <summary>
/// The commands that link a Minecraft account to a Discord account.
/// </summary>
public sealed class LinkCommands : CrossModModule {

	/**
	 * Starts a link and hands out the code.
	 *
	 * @return the task of the command
	 */
	[SlashCommand("link", "Link your Minecraft account to your Discord account")]
	public async Task LinkAsync() {
		IUser user = Context.User;
		string code = await BotServices.Links.StartAsync(user.Id, user.Username);

		string text = "Your CrossMod link code is `" + code + "`.\n"
				+ "Open Minecraft, send `.link " + code + "` in the chat and the accounts are linked.";

		// The direct messages of the member are closed quite often, the code is
		// shown in the answer in any case.
		try {
			IDMChannel channel = await user.CreateDMChannelAsync();
			await channel.SendMessageAsync(text);
		} catch (Exception) {
			// The member does not want direct messages, never mind.
		}

		await RespondAsync(text);
	}

	/**
	 * Removes the link of the member.
	 *
	 * @return the task of the command
	 */
	[SlashCommand("unlink", "Remove the link between your Minecraft and Discord account")]
	public async Task UnlinkAsync() {
		LinkRecord? record = BotServices.Links.FindByDiscord(Context.User.Id);

		if (record is null) {
			await RespondAsync("Your account is not linked.");
			return;
		}

		await BotServices.Links.UnlinkAsync(Context.User.Id);

		if (GuildId is ulong guildId) {
			await BotServices.Verification.RevokeAsync(guildId, Context.User.Id);
		}

		await RespondAsync("The link to `" + record.MinecraftName + "` was removed.");
	}

	/**
	 * Shows the link of the member.
	 *
	 * @return the task of the command
	 */
	[SlashCommand("profile", "Show the Minecraft account that is linked to your Discord account")]
	public async Task ProfileAsync() {
		LinkRecord? record = BotServices.Links.FindByDiscord(Context.User.Id);

		if (record is null) {
			await RespondAsync("Your account is not linked. Run `/link` to link it.");
			return;
		}

		EmbedBuilder embed = new EmbedBuilder()
				.WithTitle(record.MinecraftName)
				.WithDescription("Linked to " + Context.User.Username)
				.WithColor(AnnouncementService.Accent)
				.AddField("Minecraft", record.MinecraftName, true)
				.AddField("Uuid", record.MinecraftUuid, true)
				.AddField("Linked", "<t:" + record.LinkedAt.ToUnixTimeSeconds() + ":R>", true);

		await RespondAsync(embed.Build());
	}
}
