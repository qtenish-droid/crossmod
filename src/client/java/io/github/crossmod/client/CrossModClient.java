package io.github.crossmod.client;

import io.github.crossmod.CrossMod;
import io.github.crossmod.client.dev.DeveloperHooks;
import io.github.crossmod.client.discord.DiscordChatCommands;
import io.github.crossmod.client.gui.screen.CrossModScreen;
import io.github.crossmod.client.gui.screen.GlassScreen;
import io.github.crossmod.client.gui.theme.Theme;
import io.github.crossmod.client.keybind.KeyBinds;
import io.github.crossmod.client.module.Module;
import io.github.crossmod.client.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Client entry point for CrossMod.
 *
 * <p>Everything CrossMod does is client side, therefore this initializer wires
 * up the client only services: the key bindings, the module registry and the
 * heads up display overlays. The shared, environment agnostic services
 * (configuration storage) are bootstrapped by
 * {@code io.github.crossmod.CrossMod} in the common source set.</p>
 */
public final class CrossModClient implements ClientModInitializer {

	/** The registry of all modules. */
	private static final ModuleManager MODULE_MANAGER = new ModuleManager();

	/** The developer hooks that verify the interface automatically. */
	private static final DeveloperHooks DEVELOPER_HOOKS = new DeveloperHooks(MODULE_MANAGER);

	/** The chat command that links the account of the player to Discord. */
	private static final DiscordChatCommands DISCORD_COMMANDS = new DiscordChatCommands();

	@Override
	public void onInitializeClient() {
		loadTheme();

		MODULE_MANAGER.registerBuiltIns();
		MODULE_MANAGER.load();

		KeyMappingHelper.registerKeyMapping(KeyBinds.OPEN_GUI);

		DISCORD_COMMANDS.register();

		ClientTickEvents.END_CLIENT_TICK.register(this::onEndTick);

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("crossmod", "overlays"), this::renderHud);
	}

	/**
	 * Returns the registry of all modules.
	 *
	 * @return the module manager
	 */
	public static ModuleManager moduleManager() {
		return MODULE_MANAGER;
	}

	/**
	 * Handles the client tick: module ticks, the interface key binding and the
	 * persistence of the module states.
	 *
	 * @param minecraft the client
	 */
	private void onEndTick(Minecraft minecraft) {
		MODULE_MANAGER.tick();

		DEVELOPER_HOOKS.tick(minecraft);

		while (KeyBinds.OPEN_GUI.consumeClick()) {
			if (minecraft.gui.screen() instanceof GlassScreen) {
				minecraft.setScreenAndShow(null);
			} else {
				minecraft.setScreenAndShow(new CrossModScreen(MODULE_MANAGER));
			}
		}
	}

	/**
	 * Draws the heads up display elements of all enabled modules.
	 *
	 * @param graphics     the graphics context
	 * @param deltaTracker the delta tracker of the current frame
	 */
	private void renderHud(net.minecraft.client.gui.GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker deltaTracker) {
		if (Minecraft.getInstance().gui.screen() instanceof GlassScreen) {
			return;
		}

		for (Module module : MODULE_MANAGER.modules()) {
			if (module.isEnabled() && module.hasHud()) {
				module.renderHud(graphics, deltaTracker);
			}
		}
	}

	/**
	 * Reads the appearance of the interface from the configuration.
	 */
	private void loadTheme() {
		Theme theme = Theme.get();
		io.github.crossmod.config.ConfigManager config = CrossMod.config();

		theme.setAccent(parseColor(config.getString("gui", "accent", "#5CC8FF"), theme.accent()));
		theme.setScale(Integer.parseInt(config.getString("gui", "scale", "0")));
		theme.setDimBackground(config.getBoolean("gui", "dim_background", true));
		theme.setShowTooltips(config.getBoolean("gui", "show_tooltips", true));

		theme.addListener(changed -> {
			io.github.crossmod.config.ConfigManager store = CrossMod.config();
			store.setString("gui", "accent", String.format("#%06X", changed.accent() & 0xFFFFFF));
			store.setString("gui", "scale", String.valueOf(changed.scale()));
			store.setBoolean("gui", "dim_background", changed.dimBackground());
			store.setBoolean("gui", "show_tooltips", changed.showTooltips());
			store.save();
		});
	}

	/**
	 * Parses a colour in the {@code #RRGGBB} format.
	 *
	 * @param value    the value to parse
	 * @param fallback the colour used when the value cannot be parsed
	 * @return the parsed colour
	 */
	private int parseColor(String value, int fallback) {
		try {
			return 0xFF000000 | Integer.parseInt(value.replace("#", ""), 16);
		} catch (NumberFormatException exception) {
			return fallback;
		}
	}
}
