using CrossMod.Bot.Services;
using Discord.Interactions;

namespace CrossMod.Bot.Modules;

/// <summary>
/// The commands that post the announcements and the releases.
/// </summary>
public sealed class AnnouncementCommands : CrossModModule {

	/**
	 * Posts an announcement.
	 *
	 * @param title the title of the post
	 * @param body  the body of the post
	 * @return the task of the command
	 */
	[SlashCommand("announce", "Post an announcement in the announcement channel")]
	[RequireStaff]
	public async Task AnnounceAsync(string title, string body) {
		bool sent = await BotServices.Announcements.PostAnnouncementAsync(title, body);

		await RespondAsync(sent ? "The announcement was posted." : "There is no announcement channel.");
	}

	/**
	 * Posts a release.
	 *
	 * @param version   the version of the release
	 * @param changelog the changelog of the release
	 * @param url       the address of the release
	 * @return the task of the command
	 */
	[SlashCommand("release", "Post a release in the announcement channel")]
	[RequireStaff]
	public async Task ReleaseAsync(string version, string changelog, string? url = null) {
		bool sent = await BotServices.Announcements.PostReleaseAsync(version, changelog, url, null);

		await RespondAsync(sent ? "The release was posted." : "There is no announcement channel.");
	}
}
