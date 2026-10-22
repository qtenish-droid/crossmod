using CrossMod.Bot;
using CrossMod.Bot.Api;
using CrossMod.Bot.Configuration;
using CrossMod.Bot.Data;
using CrossMod.Bot.Discord;
using CrossMod.Bot.Services;
using Discord;
using Discord.Interactions;
using Discord.Rest;
using Discord.WebSocket;

WebApplicationBuilder builder = WebApplication.CreateBuilder(args);

builder.Logging.ClearProviders();
builder.Logging.AddSimpleConsole(options => {
	options.SingleLine = true;
	options.TimestampFormat = "HH:mm:ss ";
});

// The gateway of Discord writes what it receives into the log, which helps
// when something does not arrive.
builder.Logging.AddFilter("Discord", LogLevel.Trace);
builder.Logging.AddFilter("Discord.Net", LogLevel.Trace);

BotOptions options = BotConfig.Load(builder.Configuration);
BotConfig.Validate(options);

builder.WebHost.UseUrls($"http://0.0.0.0:{options.HttpPort}");

// The gateway connection. The members intent is needed for the welcome
// messages and the member counter, the reactions intent for the reaction roles.
GatewayIntents intents = GatewayIntents.Guilds | GatewayIntents.GuildMessageReactions;

if (options.MembersIntent) {
	intents |= GatewayIntents.GuildMembers;
}

DiscordSocketClient client = new(new DiscordSocketConfig {
	GatewayIntents = intents,
	AlwaysDownloadUsers = options.MembersIntent
});
// The services of the bot. Everything that reads from Discord uses the REST
// interface, the gateway is only used for the events.
StateStore store = new(options.DataPath);
LinkService links = new(store);
MojangService mojang = new();
DiscordRestClient rest = ((IRestClientProvider)client).RestClient;
VerificationService verification = new(options, rest);
AnnouncementService announcements = new(options, rest);
ReactionRoleService reactionRoles = new(store, rest);
WelcomeService welcome = new(options, rest);

BotServices.Initialize(options, client, rest, store, links, mojang, verification, announcements, reactionRoles, welcome);

builder.Services.AddSingleton(options);
builder.Services.AddSingleton(client);
builder.Services.AddSingleton(rest);
builder.Services.AddSingleton(store);
builder.Services.AddSingleton(links);
builder.Services.AddSingleton(mojang);
builder.Services.AddSingleton(verification);
builder.Services.AddSingleton(announcements);
builder.Services.AddSingleton(reactionRoles);
builder.Services.AddSingleton(welcome);

// The slash commands.
builder.Services.AddSingleton(provider => new InteractionService(client, new InteractionServiceConfig {
	AutoServiceScopes = true,
	DefaultRunMode = RunMode.Async
}));

// The gateway itself runs as a hosted service.
builder.Services.AddSingleton<DiscordService>();
builder.Services.AddHostedService(provider => provider.GetRequiredService<DiscordService>());

WebApplication app = builder.Build();

app.MapBotApi(options);

app.Run();
