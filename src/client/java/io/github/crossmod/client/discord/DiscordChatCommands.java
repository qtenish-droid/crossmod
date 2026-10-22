package io.github.crossmod.client.discord;

import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * The chat command that links the Minecraft account of the player to the
 * Discord account of the player.
 *
 * <p>The player runs {@code /link} in Discord, receives a code and types
 * {@code .link <code>} in the chat. The message never reaches the server, the
 * mod hands the code over to the bot and reports the answer of the bot in the
 * chat.</p>
 */
public final class DiscordChatCommands {

	/** The prefix of the command. */
	public static final String PREFIX = ".link ";

	/** The client that talks to the bot. */
	private final DiscordLinkClient client = new DiscordLinkClient();

	/**
	 * Registers the command.
	 */
	public void register() {
		ClientSendMessageEvents.ALLOW_CHAT.register(this::allowChat);
	}

	/**
	 * Handles the chat messages of the player.
	 *
	 * @param message the message the player sends
	 * @return whether the message is sent to the server
	 */
	private boolean allowChat(String message) {
		if (!message.startsWith(PREFIX)) {
			return true;
		}

		String code = message.substring(PREFIX.length()).trim();

		if (code.isEmpty()) {
			say("Usage: .link <code>");
			return false;
		}

		if (!client.isConfigured()) {
			say("The CrossMod bot is not configured. Set the address and the key of the bot in the configuration file.");
			return false;
		}

		say("Linking your account ...");

		String name = Minecraft.getInstance().getUser().getName();

		client.link(code, name).whenComplete((result, failure) -> {
			if (failure != null) {
				say("The bot could not be reached: " + failure.getMessage());
				return;
			}

			say(result.message());
		});

		return false;
	}

	/**
	 * Writes a line into the chat of the player.
	 *
	 * @param text the text of the line
	 */
	private static void say(String text) {
		Minecraft.getInstance().execute(() -> Minecraft.getInstance().gui.hud
				.getChat()
				.addClientSystemMessage(Component.literal(text)));
	}
}
