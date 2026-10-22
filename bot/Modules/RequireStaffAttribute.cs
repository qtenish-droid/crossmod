using CrossMod.Bot.Services;
using Discord;
using Discord.Interactions;
using Discord.WebSocket;
using Microsoft.Extensions.DependencyInjection;

namespace CrossMod.Bot.Modules;

/// <summary>
/// Allows a command to be used by the staff role only.
/// </summary>
public sealed class RequireStaffAttribute : PreconditionAttribute {

	/**
	 * Checks whether the member may use the command.
	 *
	 * @param context     the context of the interaction
	 * @param commandInfo the command
	 * @param services    the services of the bot
	 * @return whether the member is staff
	 */
	public override async Task<PreconditionResult> CheckRequirementsAsync(IInteractionContext context, ICommandInfo commandInfo, IServiceProvider services) {
		VerificationService verification = services.GetRequiredService<VerificationService>();

		ulong? guildId = context.Guild?.Id ?? BotServices.Options.GuildId;

		if (guildId is not null && await verification.IsStaffAsync(guildId.Value, context.User.Id)) {
			return PreconditionResult.FromSuccess();
		}

		return PreconditionResult.FromError("This command is reserved for the staff of the server.");
	}
}
