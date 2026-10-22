package io.github.crossmod.client.dev;

import io.github.crossmod.client.gui.screen.CrossModScreen;
import io.github.crossmod.client.gui.screen.GuiSettingsScreen;
import io.github.crossmod.client.gui.screen.ModuleSettingsScreen;
import io.github.crossmod.client.module.Module;
import io.github.crossmod.client.module.ModuleManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Developer utilities that make the interface verifiable without a human at
 * the keyboard.
 *
 * <p>The hooks are completely inert unless one of the following environment
 * variables is set, so a normal installation never behaves differently:</p>
 *
 * <ul>
 *     <li>{@code CROSSMOD_DEV_OPEN_GUI} - opens the CrossMod interface after the
 *     given amount of ticks (default 60).</li>
 *     <li>{@code CROSSMOD_DEV_OPEN_MODULE_SETTINGS} - opens the settings of the
 *     first module instead.</li>
 *     <li>{@code CROSSMOD_DEV_OPEN_GUI_SETTINGS} - opens the settings of the
 *     interface instead.</li>
 *     <li>{@code CROSSMOD_DEV_SCREENSHOT} - takes a screenshot of the game a
 *     few ticks after the interface was opened.</li>
 * </ul>
 *
 * <p>They are used by the continuous integration setup to verify that the
 * interface still renders and to collect reference images of it.</p>
 */
@Environment(EnvType.CLIENT)
public final class DeveloperHooks {

	private static final Logger LOGGER = LoggerFactory.getLogger("CrossMod/Dev");

	private static final String OPEN_GUI_VARIABLE = "CROSSMOD_DEV_OPEN_GUI";
	private static final String OPEN_MODULE_SETTINGS_VARIABLE = "CROSSMOD_DEV_OPEN_MODULE_SETTINGS";
	private static final String OPEN_GUI_SETTINGS_VARIABLE = "CROSSMOD_DEV_OPEN_GUI_SETTINGS";
	private static final String SCREENSHOT_VARIABLE = "CROSSMOD_DEV_SCREENSHOT";

	private static final int DEFAULT_OPEN_DELAY = 60;
	private static final int SCREENSHOT_DELAY = 12;

	private final ModuleManager moduleManager;

	private int ticks;
	private boolean screenshotTaken;
	private boolean worldStaged;

	public DeveloperHooks(ModuleManager moduleManager) {
		this.moduleManager = moduleManager;
	}

	/**
	 * Runs the developer hooks.
	 *
	 * @param minecraft the client
	 */
	public void tick(Minecraft minecraft) {
		if (!isEnabled()) {
			return;
		}

		// Automated captures need a real world behind the interface. Without
		// one the title panorama is still visible, but the blur has no world
		// frame to work with and would feed the previous interface back in.
		if (System.getenv(SCREENSHOT_VARIABLE) != null && minecraft.player == null) {
			return;
		}

		this.ticks++;

		if (System.getenv(SCREENSHOT_VARIABLE) != null && !this.worldStaged
				&& this.ticks == Math.max(1, openDelay() - 60)) {
			this.worldStaged = true;
			stageWorld(minecraft);
		}

		if (this.ticks == openDelay()) {
			// Screenshots are taken at the same virtual size as the reference
			// client. This only affects automated captures.
			if (System.getenv(SCREENSHOT_VARIABLE) != null && minecraft.options.guiScale().get() != 2) {
				minecraft.options.guiScale().set(2);
				minecraft.resizeGui();
			}

			open(minecraft);
		}

		if (this.ticks == openDelay() + SCREENSHOT_DELAY && !this.screenshotTaken) {
			this.screenshotTaken = true;
			LOGGER.info("[CrossMod] Taking the developer screenshot.");
			Screenshot.grab(minecraft, false);
		}
	}

	/**
	 * Moves automated captures to a bright surface scene so the glass and the
	 * blur can be evaluated against more than a dark cave.
	 *
	 * @param minecraft the client
	 */
	private void stageWorld(Minecraft minecraft) {
		MinecraftServer server = minecraft.getSingleplayerServer();

		if (server == null) {
			return;
		}

		var commands = server.getCommands();
		var source = server.createCommandSourceStack();

		commands.performPrefixedCommand(source, "time set day");
		commands.performPrefixedCommand(source, "weather clear");
		commands.performPrefixedCommand(source, "gamemode spectator @p");
		commands.performPrefixedCommand(source, "tp @p 0 200 0");
		LOGGER.info("[CrossMod] Staged the developer capture high above the world.");
	}

	/**
	 * Opens the screen that the developer hook asks for.
	 *
	 * @param minecraft the client
	 */
	private void open(Minecraft minecraft) {
		if (minecraft.player != null) {
			minecraft.player.setYRot(0.0F);
			minecraft.player.setXRot(10.0F);
			minecraft.player.setYHeadRot(0.0F);
		}

		if (System.getenv(OPEN_MODULE_SETTINGS_VARIABLE) != null) {
			if (this.moduleManager.modules().isEmpty()) {
				LOGGER.info("[CrossMod] No modules are registered; opening the interface instead.");
				minecraft.setScreenAndShow(new CrossModScreen(this.moduleManager));
				return;
			}

			LOGGER.info("[CrossMod] Opening the module settings for the developer screenshot.");
			Module module = this.moduleManager.modules().get(0);
			minecraft.setScreenAndShow(new ModuleSettingsScreen(module, this.moduleManager));
			return;
		}

		if (System.getenv(OPEN_GUI_SETTINGS_VARIABLE) != null) {
			LOGGER.info("[CrossMod] Opening the interface settings for the developer screenshot.");
			minecraft.setScreenAndShow(new GuiSettingsScreen(this.moduleManager));
			return;
		}

		LOGGER.info("[CrossMod] Opening the interface for the developer screenshot.");
		minecraft.setScreenAndShow(new CrossModScreen(this.moduleManager));
	}

	/**
	 * Returns whether any developer hook is active.
	 *
	 * @return {@code true} when a hook is active
	 */
	private boolean isEnabled() {
		return System.getenv(OPEN_GUI_VARIABLE) != null
				|| System.getenv(OPEN_MODULE_SETTINGS_VARIABLE) != null
				|| System.getenv(OPEN_GUI_SETTINGS_VARIABLE) != null
				|| System.getenv(SCREENSHOT_VARIABLE) != null;
	}

	/**
	 * Reads the delay after which the screen is opened.
	 *
	 * @return the delay in ticks
	 */
	private int openDelay() {
		String value = System.getenv(OPEN_GUI_VARIABLE);

		if (value == null || value.isBlank()) {
			value = System.getenv(OPEN_MODULE_SETTINGS_VARIABLE);
		}

		if (value == null || value.isBlank()) {
			value = System.getenv(OPEN_GUI_SETTINGS_VARIABLE);
		}

		if (value == null || value.isBlank()) {
			return DEFAULT_OPEN_DELAY;
		}

		try {
			return Math.max(1, Integer.parseInt(value.trim()));
		} catch (NumberFormatException exception) {
			return DEFAULT_OPEN_DELAY;
		}
	}
}
