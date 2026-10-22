using Discord;
using Discord.Rest;

namespace CrossMod.Bot.Services;

/// <summary>
/// Grants and revokes the role that marks a linked member.
/// </summary>
public sealed class VerificationService {

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
	public VerificationService(Configuration.BotOptions options, DiscordRestClient client) {
		_options = options;
		_client = client;
	}

	/**
	 * Grants the verified role to a member.
	 *
	 * @param guildId the id of the guild
	 * @param userId  the id of the member
	 * @return whether the role was granted
	 */
	public async Task<bool> GrantAsync(ulong guildId, ulong userId) {
		if (_options.VerifiedRoleId is null) {
			return false;
		}

		IGuildUser? member = await FindAsync(guildId, userId);

		if (member is null) {
			return false;
		}

		if (member.RoleIds.Contains(_options.VerifiedRoleId.Value)) {
			return true;
		}

		await member.AddRoleAsync(_options.VerifiedRoleId.Value);
		return true;
	}

	/**
	 * Takes the verified role away from a member.
	 *
	 * @param guildId the id of the guild
	 * @param userId  the id of the member
	 * @return whether the role was removed
	 */
	public async Task<bool> RevokeAsync(ulong guildId, ulong userId) {
		if (_options.VerifiedRoleId is null) {
			return false;
		}

		IGuildUser? member = await FindAsync(guildId, userId);

		if (member is null || !member.RoleIds.Contains(_options.VerifiedRoleId.Value)) {
			return false;
		}

		await member.RemoveRoleAsync(_options.VerifiedRoleId.Value);
		return true;
	}

	/**
	 * Checks whether a member is staff.
	 *
	 * @param guildId the id of the guild
	 * @param userId  the id of the member
	 * @return whether the member may use the staff commands
	 */
	public async Task<bool> IsStaffAsync(ulong guildId, ulong userId) {
		if (_options.StaffRoleId is null) {
			return false;
		}

		IGuildUser? member = await FindAsync(guildId, userId);

		return member is not null && member.RoleIds.Contains(_options.StaffRoleId.Value);
	}

	/**
	 * Returns a member of a guild.
	 *
	 * @param guildId the id of the guild
	 * @param userId  the id of the member
	 * @return the member, or {@code null}
	 */
	public async Task<IGuildUser?> FindAsync(ulong guildId, ulong userId) {
		IGuild? guild = await _client.GetGuildAsync(guildId);

		if (guild is null) {
			return null;
		}

		return await guild.GetUserAsync(userId);
	}
}
