using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using CrossMod.Bot.Configuration;
using CrossMod.Bot.Data;
using CrossMod.Bot.Services;
using Discord;
using Discord.Rest;

namespace CrossMod.Bot.Api;

/// <summary>
/// The body of a link request, sent by the client of the mod.
/// </summary>
public sealed class LinkRequest {

	/** The code the player got from the `/link` command. */
	public string? Code { get; set; }

	/** The name of the Minecraft account. */
	public string? MinecraftName { get; set; }
}

/// <summary>
/// The body of a publish request, sent by the build pipeline.
/// </summary>
public sealed class PublishRequest {

	/** The version of the release. */
	public string? Version { get; set; }

	/** The changelog of the release. */
	public string? Changelog { get; set; }

	/** The address of the release. */
	public string? Url { get; set; }

	/** The title of the post. */
	public string? Title { get; set; }
}

/// <summary>
/// The HTTP endpoints of the bot.
/// </summary>
public static class BotApi {

	/** The moment the bot was started. */
	private static readonly DateTimeOffset StartedAt = DateTimeOffset.UtcNow;

	/**
	 * Adds the endpoints to the host.
	 *
	 * @param app     the host
	 * @param options the options of the bot
	 */
	public static void MapBotApi(this WebApplication app, BotOptions options) {
		app.MapGet("/health", () => Results.Ok(new {
			status = "ok",
			uptime = (DateTimeOffset.UtcNow - StartedAt).ToString()
		}));

		app.MapGet("/api/status", async (DiscordRestClient client, LinkService links) => {
			IGuild? guild = options.GuildId is null ? null : await client.GetGuildAsync(options.GuildId.Value, true);

			return Results.Ok(new {
				name = "CrossMod",
				guild = guild?.Name,
				members = guild?.ApproximateMemberCount ?? 0,
				links = links.Links().Count,
				uptime = (DateTimeOffset.UtcNow - StartedAt).ToString()
			});
		});

		app.MapPost("/api/link", async (
				HttpRequest request,
				LinkService links,
				MojangService mojang,
				VerificationService verification,
				CancellationToken cancellationToken) => {
			if (!Authorized(request, options)) {
				return Results.Unauthorized();
			}

			LinkRequest? body = await request.ReadFromJsonAsync<LinkRequest>(cancellationToken);

			if (body is null || string.IsNullOrWhiteSpace(body.Code) || string.IsNullOrWhiteSpace(body.MinecraftName)) {
				return Results.BadRequest(new { error = "The body needs a code and a minecraftName." });
			}

			PendingLink? pending = links.FindPending(body.Code);

			if (pending is null) {
				return Results.BadRequest(new { error = "That code is unknown or has expired." });
			}

			MinecraftProfile? profile = await mojang.ResolveAsync(body.MinecraftName);

			if (profile is null) {
				return Results.BadRequest(new { error = "There is no Minecraft account named " + body.MinecraftName + "." });
			}

			LinkResult result = await links.CompleteAsync(body.Code, profile.Uuid, profile.Name);

			if (!result.Success) {
				return Results.BadRequest(new { error = result.Message });
			}

			if (options.GuildId is not null) {
				await verification.GrantAsync(options.GuildId.Value, pending.DiscordUserId);
			}

			return Results.Ok(new {
				ok = true,
				minecraftName = profile.Name,
				minecraftUuid = profile.Uuid
			});
		});

		app.MapPost("/api/publish", async (
				HttpRequest request,
				AnnouncementService announcements,
				CancellationToken cancellationToken) => {
			if (!Authorized(request, options)) {
				return Results.Unauthorized();
			}

			PublishRequest? body = await request.ReadFromJsonAsync<PublishRequest>(cancellationToken);

			if (body is null || string.IsNullOrWhiteSpace(body.Version)) {
				return Results.BadRequest(new { error = "The body needs a version." });
			}

			bool sent = await announcements.PostReleaseAsync(
					body.Version,
					body.Changelog ?? string.Empty,
					body.Url,
					body.Title);

			return sent
					? Results.Ok(new { ok = true })
					: Results.Problem("There is no announcement channel.", statusCode: 500);
		});

		app.MapPost("/api/github", async (
				HttpRequest request,
				AnnouncementService announcements,
				CancellationToken cancellationToken) => {
			string body;

			using (StreamReader reader = new StreamReader(request.Body, Encoding.UTF8)) {
				body = await reader.ReadToEndAsync(cancellationToken);
			}

			if (!string.IsNullOrWhiteSpace(options.GitHubWebhookSecret)
					&& !Signed(options.GitHubWebhookSecret, request.Headers["X-Hub-Signature-256"].ToString(), body)) {
				return Results.Unauthorized();
			}

			using JsonDocument document = JsonDocument.Parse(body);

			if (!document.RootElement.TryGetProperty("release", out JsonElement release)) {
				return Results.Ok(new { ok = true, ignored = "not a release" });
			}

			string action = document.RootElement.TryGetProperty("action", out JsonElement value) ? value.GetString() ?? string.Empty : string.Empty;

			if (!action.Equals("published", StringComparison.OrdinalIgnoreCase)) {
				return Results.Ok(new { ok = true, ignored = action });
			}

			string version = Property(release, "tag_name") ?? "unknown";
			string name = Property(release, "name") ?? version;
			string notes = Property(release, "body") ?? "A new version of CrossMod is out.";
			string? url = Property(release, "html_url");

			bool sent = await announcements.PostReleaseAsync(version, notes, url, name);

			return sent
					? Results.Ok(new { ok = true })
					: Results.Problem("There is no announcement channel.", statusCode: 500);
		});
	}

	/**
	 * Reads a string property of a JSON object.
	 *
	 * @param element the object
	 * @param name    the name of the property
	 * @return the value, or {@code null}
	 */
	private static string? Property(JsonElement element, string name) {
		if (!element.TryGetProperty(name, out JsonElement value)) {
			return null;
		}

		return value.ValueKind == JsonValueKind.String ? value.GetString() : value.ToString();
	}

	/**
	 * Checks the api key of a request.
	 *
	 * @param request the request
	 * @param options the options of the bot
	 * @return whether the request may continue
	 */
	private static bool Authorized(HttpRequest request, BotOptions options) {
		string? key = request.Headers["X-Api-Key"].ToString();

		if (string.IsNullOrEmpty(key)) {
			return false;
		}

		byte[] given = Encoding.UTF8.GetBytes(key);
		byte[] expected = Encoding.UTF8.GetBytes(options.ApiKey);

		return given.Length == expected.Length && CryptographicOperations.FixedTimeEquals(given, expected);
	}

	/**
	 * Checks the signature of a GitHub webhook.
	 *
	 * @param secret    the secret of the webhook
	 * @param signature the signature of the request
	 * @param body      the body of the request
	 * @return whether the signature is correct
	 */
	private static bool Signed(string secret, string signature, string body) {
		if (string.IsNullOrWhiteSpace(signature)) {
			return false;
		}

		using HMACSHA256 hmac = new(Encoding.UTF8.GetBytes(secret));
		byte[] hash = hmac.ComputeHash(Encoding.UTF8.GetBytes(body));
		string expected = "sha256=" + Convert.ToHexString(hash).ToLowerInvariant();

		return string.Equals(expected, signature.Trim(), StringComparison.OrdinalIgnoreCase);
	}
}
