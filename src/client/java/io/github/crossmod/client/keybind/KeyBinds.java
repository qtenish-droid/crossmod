package io.github.crossmod.client.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/**
 * The key bindings of CrossMod.
 *
 * <p>Every binding is registered with the normal Minecraft key binding system,
 * which means that all of them can be changed in the controls screen of the
 * game.</p>
 */
public final class KeyBinds {

	/** The category that all CrossMod bindings are grouped under. */
	public static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath("crossmod", "main"));

	/** Opens and closes the CrossMod interface. */
	public static final KeyMapping OPEN_GUI = new KeyMapping(
			"key.crossmod.open_gui",
			InputConstants.Type.KEYBOARD,
			InputConstants.KEY_RSHIFT,
			CATEGORY
	);

	private KeyBinds() {
	}
}
