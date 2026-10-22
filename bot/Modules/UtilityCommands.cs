using CrossMod.Bot.Services;
using Discord;
using Discord.Interactions;

namespace CrossMod.Bot.Modules;

/// <summary>
/// The commands that everybody can use.
/// </summary>
public sealed class UtilityCommands : CrossModModule {

	/**
	 * Lists the commands of the bot.
	 *
	 * @return the task of the command
	 */
	[SlashCommand("help", "Show the commands of the bot")]
	public async Task HelpAsync() {
		EmbedBuilder embed = new EmbedBuilder()
				.WithTitle("CrossMod")
				.WithDescription("The bot of the CrossMod server.")
				.WithColor(AnnouncementService.Accent)
				.AddField("/link", "Link your Minecraft account to your Discord account.", true)
				.AddField("/unlink", "Remove the link again.", true)
				.AddField("/profile", "Show the Minecraft account that is linked to you.", true)
				.AddField("/server", "Show the member count of the server.", true)
				.AddField("Staff", "/announce, /release, /verify, /reactionrole", false);

		await RespondAsync(embed.Build());
	}

	/**
	 * Shows the member count of the server.
	 *
	 * @return the task of the command
	 */
	[SlashCommand("server", "Show the member count of the server")]
	public async Task ServerAsync() {
		string name = "The CrossMod server";

		if (GuildId is ulong guildId) {
			IGuild? guild = await BotServices.Rest.GetGuildAsync(guildId, true);

			if (guild is not null) {
				name = guild.Name;
			}
		}

		EmbedBuilder embed = new EmbedBuilder()
				.WithTitle(name)
				.WithColor(AnnouncementService.Accent)
				.WithTimestamp(DateTimeOffset.UtcNow);

		await RespondAsync(embed.Build());
	}
}
