using CrossMod.Bot.Services;
using Discord;
using Discord.Interactions;
using Discord.WebSocket;

namespace CrossMod.Bot.Modules;

/// <summary>
/// The base of every command of the bot.
///
/// <para>It gives the commands a short way to answer and a short way to find
/// out whether the member that called them is staff.</para>
/// </summary>
public abstract class CrossModModule : InteractionModuleBase {

	/**
	 * Answers the command.
	 *
	 * @param text      the text of the answer
	 * @param embed     the embed of the answer, may be {@code null}
	 * @param ephemeral whether only the member sees the answer
	 * @return the task of the answer
	 */
	protected Task RespondAsync(string text, Embed? embed = null, bool ephemeral = true) {
		return Context.Interaction.RespondAsync(text: text, embed: embed, ephemeral: ephemeral);
	}

	/**
	 * Answers the command with an embed.
	 *
	 * @param embed     the embed of the answer
	 * @param ephemeral whether only the member sees the answer
	 * @return the task of the answer
	 */
	protected Task RespondAsync(Embed embed, bool ephemeral = true) {
		return Context.Interaction.RespondAsync(embed: embed, ephemeral: ephemeral);
	}

	/**
	 * Returns the id of the guild the command was used in.
	 *
	 * @return the id of the guild, or {@code null} in a direct message
	 */
	protected ulong? GuildId {
		get {
			if (Context.Guild is not null) {
				return Context.Guild.Id;
			}

			return BotServices.Options.GuildId;
		}
	}

	/**
	 * Checks whether the member that used the command is staff.
	 *
	 * @return whether the member is staff
	 */
	protected async Task<bool> IsStaffAsync() {
		ulong? guildId = GuildId;

		if (guildId is null) {
			return false;
		}

		return await BotServices.Verification.IsStaffAsync(guildId.Value, Context.User.Id);
	}

	/**
	 * Reads an id out of a mention or a plain id.
	 *
	 * <p>Discord shows the id of a member as {@code <@123456789>}, of a role as
	 * {@code <@&123456789>} and of a channel as {@code <#123456789>}. The
	 * command accepts all of them and a plain id as well.</p>
	 *
	 * @param value the value the member typed
	 * @return the id, or {@code null} when the value is no id
	 */
	protected static ulong? ParseId(string? value) {
		if (string.IsNullOrWhiteSpace(value)) {
			return null;
		}

		string cleaned = value.Trim();

		int start = cleaned.IndexOfAny(new[] { '<', '&', '#' });
		int end = cleaned.IndexOf('>');

		if (start >= 0 && end > start) {
			cleaned = cleaned.Substring(start + 1, end - start - 1);
		}

		return ulong.TryParse(cleaned, out ulong id) ? id : null;
	}
}
