using CrossMod.Bot.Configuration;
using Discord;
using Discord.Interactions;
using Discord.WebSocket;
using Microsoft.Extensions.Logging;

namespace CrossMod.Bot.Discord;

/// <summary>
/// Owns the gateway connection and the events of the bot.
/// </summary>
public sealed class DiscordService : IHostedService {

	/** The options of the bot. */
	private readonly BotOptions _options;

	/** The client of the bot. */
	private readonly DiscordSocketClient _client;

	/** The service that runs the slash commands. */
	private readonly InteractionService _interactions;

	/** The services of the host. */
	private readonly IServiceProvider _services;

	/** The logger of the bot. */
	private readonly ILogger<DiscordService> _logger;

	/**
	 * Creates the service.
	 *
	 * @param options      the options of the bot
	 * @param client       the client of the bot
	 * @param interactions the service that runs the slash commands
	 * @param services     the services of the host
	 * @param logger       the logger of the bot
	 */
	public DiscordService(
			BotOptions options,
			DiscordSocketClient client,
			InteractionService interactions,
			IServiceProvider services,
			ILogger<DiscordService> logger) {
		_options = options;
		_client = client;
		_interactions = interactions;
		_services = services;
		_logger = logger;
	}

	/**
	 * Connects the bot and registers the slash commands.
	 *
	 * @param cancellationToken the token that stops the start
	 */
	public async Task StartAsync(CancellationToken cancellationToken) {
		_client.InteractionCreated += OnInteractionCreated;
		_client.UserJoined += OnUserJoined;
		_client.UserLeft += OnUserLeft;
		_client.ReactionAdded += OnReactionAdded;
		_client.ReactionRemoved += OnReactionRemoved;
		_client.Disconnected += OnDisconnected;

		if (_options.Offline) {
			_logger.LogWarning("The bot runs offline, no command is registered.");
			return;
		}

		await _client.LoginAsync(TokenType.Bot, _options.DiscordToken, true);
		await _client.StartAsync();

		// The commands may only be registered once the gateway is ready, the
		// client refuses to work before that.
		await WaitUntilReadyAsync(cancellationToken);

		_logger.LogInformation("CrossMod is connected as {User}.", _client.CurrentUser?.Username ?? "unknown");

		await RegisterCommandsAsync();
		await BotServices.MemberCount.UpdateAsync();
	}

	/**
	 * Disconnects the bot.
	 *
	 * @param cancellationToken the token that stops the stop
	 */
	public async Task StopAsync(CancellationToken cancellationToken) {
		if (_client.LoginState == LoginState.LoggedIn) {
			await _client.LogoutAsync();
		}

		await _client.DisposeAsync();
	}

	/**
	 * Waits for the gateway to become ready.
	 *
	 * @param cancellationToken the token that stops the wait
	 */
	private async Task WaitUntilReadyAsync(CancellationToken cancellationToken) {
		TaskCompletionSource<bool> ready = new(TaskCreationOptions.RunContinuationsAsynchronously);

		_client.Ready += () => {
			ready.TrySetResult(true);
			return Task.CompletedTask;
		};

		await Task.WhenAny(
				ready.Task,
				Task.Delay(TimeSpan.FromSeconds(30), cancellationToken)).ConfigureAwait(false);

		if (!ready.Task.IsCompleted) {
			throw new TimeoutException("The gateway of Discord did not become ready within 30 seconds.");
		}

		await ready.Task.ConfigureAwait(false);
	}

	/**
	 * Registers the slash commands.
	 *
	 * <p>Guild commands show up right away, global commands take up to an hour
	 * to reach every server.</p>
	 */
	private async Task RegisterCommandsAsync() {
		await _interactions.AddModulesAsync(typeof(DiscordService).Assembly, _services);

		if (_options.GuildId is ulong guildId) {
			await _interactions.RegisterCommandsToGuildAsync(guildId, true);
			_logger.LogInformation("Registered the slash commands in the guild {Guild}.", guildId);
		} else {
			await _interactions.RegisterCommandsGloballyAsync(true);
			_logger.LogInformation("Registered the slash commands globally.");
		}
	}

	/**
	 * Runs a slash command.
	 *
	 * @param interaction the interaction
	 */
	private async Task OnInteractionCreated(SocketInteraction interaction) {
		try {
			SocketInteractionContext context = new(_client, interaction);
			await _interactions.ExecuteCommandAsync(context, _services);
		} catch (Exception exception) {
			_logger.LogError(exception, "A command of the bot failed.");
		}
	}

	/**
	 * Welists a member and updates the member counter.
	 *
	 * @param member the member that joined
	 */
	private async Task OnUserJoined(SocketGuildUser member) {
		try {
			await BotServices.Welcome.WelcomeAsync(member);
		} catch (Exception exception) {
			_logger.LogError(exception, "The welcome message failed.");
		}

		await BotServices.MemberCount.UpdateAsync();
	}

	/**
	 * Updates the member counter when a member leaves.
	 *
	 * @param guild the guild the member left
	 * @param user  the member that left
	 */
	private async Task OnUserLeft(SocketGuild guild, SocketUser user) {
		await BotServices.MemberCount.UpdateAsync();
	}

	/**
	 * Grants the roles of a reaction.
	 *
	 * @param message  the message that was reacted on
	 * @param channel  the channel of the message
	 * @param reaction the reaction that was added
	 */
	private async Task OnReactionAdded(Cacheable<IUserMessage, ulong> message, Cacheable<IMessageChannel, ulong> channel, SocketReaction reaction) {
		try {
			await BotServices.ReactionRoles.GrantAsync(message.Id, channel.Id, reaction.Emote, reaction.UserId);
		} catch (Exception exception) {
			_logger.LogError(exception, "The reaction role could not be granted.");
		}
	}

	/**
	 * Takes the roles of a reaction away.
	 *
	 * @param message  the message that was reacted on
	 * @param channel  the channel of the message
	 * @param reaction the reaction that was removed
	 */
	private async Task OnReactionRemoved(Cacheable<IUserMessage, ulong> message, Cacheable<IMessageChannel, ulong> channel, SocketReaction reaction) {
		try {
			await BotServices.ReactionRoles.RevokeAsync(message.Id, channel.Id, reaction.Emote, reaction.UserId);
		} catch (Exception exception) {
			_logger.LogError(exception, "The reaction role could not be removed.");
		}
	}

	/**
	 * Writes a lost connection into the log.
	 *
	 * @param exception the reason of the disconnect
	 * @return the task of the handler
	 */
	private Task OnDisconnected(Exception exception) {
		_logger.LogWarning(exception, "The gateway of Discord was closed, the bot reconnects on its own.");
		return Task.CompletedTask;
	}
}
