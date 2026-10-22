using CrossMod.Bot.Data;

namespace CrossMod.Bot.Services;

/// <summary>
/// The result of a link attempt.
/// </summary>
public sealed class LinkResult {

	/** Whether the link succeeded. */
	public bool Success { get; init; }

	/** The message that is shown to the user. */
	public string Message { get; init; } = string.Empty;

	/** The Discord user the link belongs to, when it succeeded. */
	public LinkRecord? Record { get; init; }
}

/// <summary>
/// Keeps the linked accounts and the codes that are waiting for the client.
/// </summary>
public sealed class LinkService {

	/** The state of the bot. */
	private readonly StateStore _store;

	/**
	 * Creates the service.
	 *
	 * @param store the state of the bot
	 */
	public LinkService(StateStore store) {
		_store = store;
	}

	/**
	 * Starts a link and returns the code the player has to type in the client.
	 *
	 * <p>Any older code of the same user is dropped, so only the newest one
	 * works. A user can only be linked to one Minecraft account at a time.</p>
	 *
	 * @param discordUserId   the id of the Discord user
	 * @param discordUsername the name of the Discord user
	 * @return the code
	 */
	public async Task<string> StartAsync(ulong discordUserId, string discordUsername) {
		string code = StateStore.NewCode();

		await _store.UpdateAsync(state => {
			state.PendingLinks.RemoveAll(pending => pending.DiscordUserId == discordUserId);
			state.Links.RemoveAll(link => link.DiscordUserId == discordUserId);
			state.PendingLinks.Add(new PendingLink {
				Code = code,
				DiscordUserId = discordUserId,
				DiscordUsername = discordUsername
			});
		}).ConfigureAwait(false);

		return code;
	}

	/**
	 * Completes a link.
	 *
	 * @param code          the code the player typed in the client
	 * @param minecraftUuid the uuid of the Minecraft account
	 * @param minecraftName the name of the Minecraft account
	 * @return the result of the attempt
	 */
	public async Task<LinkResult> CompleteAsync(string code, string minecraftUuid, string minecraftName) {
		string normalized = code.Trim().ToUpperInvariant();

		LinkResult? result = null;

		await _store.UpdateAsync(state => {
			PendingLink? pending = state.PendingLinks
					.FirstOrDefault(entry => string.Equals(entry.Code, normalized, StringComparison.OrdinalIgnoreCase));

			if (pending is null) {
				result = new LinkResult {
					Success = false,
					Message = "That code is not valid. Run `/link` in Discord to get a new one."
				};
				return;
			}

			if (pending.ExpiresAt < DateTimeOffset.UtcNow) {
				state.PendingLinks.Remove(pending);
				result = new LinkResult {
					Success = false,
					Message = "That code has expired. Run `/link` in Discord to get a new one."
				};
				return;
			}

			// A Minecraft account can only be linked to one Discord account.
			state.Links.RemoveAll(link =>
					string.Equals(link.MinecraftUuid, minecraftUuid, StringComparison.OrdinalIgnoreCase));

			LinkRecord record = new() {
				DiscordUserId = pending.DiscordUserId,
				DiscordUsername = pending.DiscordUsername,
				MinecraftUuid = minecraftUuid,
				MinecraftName = minecraftName
			};

			state.Links.Add(record);
			state.PendingLinks.Remove(pending);

			result = new LinkResult {
				Success = true,
				Message = "Linked.",
				Record = record
			};
		}).ConfigureAwait(false);

		return result ?? new LinkResult { Success = false, Message = "The link could not be completed." };
	}

	/**
	 * Links a Minecraft account to a Discord account without a code.
	 *
	 * @param discordUserId   the id of the Discord user
	 * @param discordUsername the name of the Discord user
	 * @param minecraftUuid   the uuid of the Minecraft account
	 * @param minecraftName   the name of the Minecraft account
	 * @return the result of the attempt
	 */
	public async Task<LinkResult> LinkManuallyAsync(ulong discordUserId, string discordUsername, string minecraftUuid, string minecraftName) {
		LinkResult? result = null;

		await _store.UpdateAsync(state => {
			state.Links.RemoveAll(link =>
					string.Equals(link.MinecraftUuid, minecraftUuid, StringComparison.OrdinalIgnoreCase)
					|| link.DiscordUserId == discordUserId);

			LinkRecord record = new() {
				DiscordUserId = discordUserId,
				DiscordUsername = discordUsername,
				MinecraftUuid = minecraftUuid,
				MinecraftName = minecraftName
			};

			state.Links.Add(record);
			state.PendingLinks.RemoveAll(pending => pending.DiscordUserId == discordUserId);

			result = new LinkResult {
				Success = true,
				Message = "Linked.",
				Record = record
			};
		}).ConfigureAwait(false);

		return result ?? new LinkResult { Success = false, Message = "The link could not be created." };
	}

	/**
	 * Returns the link that waits for the given code.
	 *
	 * @param code the code
	 * @return the pending link, or {@code null}
	 */
	public PendingLink? FindPending(string code) {
		string normalized = code.Trim().ToUpperInvariant();

		return _store.State.PendingLinks.FirstOrDefault(pending =>
				string.Equals(pending.Code, normalized, StringComparison.OrdinalIgnoreCase));
	}

	/**
	 * Returns all the links.
	 *
	 * @return the links
	 */
	public List<LinkRecord> Links() {
		return _store.State.Links.ToList();
	}

	/**
	 * Returns the link of a Discord user.
	 *
	 * @param discordUserId the id of the Discord user
	 * @return the link, or {@code null}
	 */
	public LinkRecord? FindByDiscord(ulong discordUserId) {
		return _store.State.Links.FirstOrDefault(link => link.DiscordUserId == discordUserId);
	}

	/**
	 * Returns the link of a Minecraft account.
	 *
	 * @param minecraftUuid the uuid of the Minecraft account
	 * @return the link, or {@code null}
	 */
	public LinkRecord? FindByMinecraft(string minecraftUuid) {
		return _store.State.Links.FirstOrDefault(link =>
				string.Equals(link.MinecraftUuid, minecraftUuid, StringComparison.OrdinalIgnoreCase));
	}

	/**
	 * Removes the link of a Discord user.
	 *
	 * @param discordUserId the id of the Discord user
	 * @return whether a link was removed
	 */
	public async Task<bool> UnlinkAsync(ulong discordUserId) {
		return await _store.UpdateAsync(state => {
			int removed = state.Links.RemoveAll(link => link.DiscordUserId == discordUserId);
			state.PendingLinks.RemoveAll(pending => pending.DiscordUserId == discordUserId);
			return removed > 0;
		}).ConfigureAwait(false);
	}

	/**
	 * Removes the links of a Minecraft account.
	 *
	 * @param minecraftUuid the uuid of the Minecraft account
	 * @return whether a link was removed
	 */
	public async Task<bool> UnlinkMinecraftAsync(string minecraftUuid) {
		return await _store.UpdateAsync(state => {
			int removed = state.Links.RemoveAll(link =>
					string.Equals(link.MinecraftUuid, minecraftUuid, StringComparison.OrdinalIgnoreCase));
			return removed > 0;
		}).ConfigureAwait(false);
	}
}
