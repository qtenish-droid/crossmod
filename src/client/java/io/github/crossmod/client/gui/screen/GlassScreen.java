package io.github.crossmod.client.gui.screen;

import io.github.crossmod.client.gui.theme.Theme;
import io.github.crossmod.client.keybind.KeyBinds;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/**
 * Base class of every CrossMod screen.
 *
 * <p>The game is never hidden behind an opaque menu. A CrossMod screen blurs
 * the game (honouring the vanilla menu blur setting) and lays a light scrim
 * over it, so the world stays visible through the glass panels that are drawn
 * on top.</p>
 */
public abstract class GlassScreen extends Screen {

	/** The strongest blur the vanilla post chain supports. */
	private static final int MAX_BLUR = 10;

	private boolean hudStateCaptured;
	private boolean hudWasHidden;
	private Integer previousBlurriness;

	protected GlassScreen(Component title) {
		super(title);
	}

	@Override
	public void added() {
		super.added();

		if (this.minecraft == null || this.minecraft.gui == null) {
			return;
		}

		if (!this.hudStateCaptured) {
			this.hudWasHidden = this.minecraft.gui.hud.isHidden();
			this.hudStateCaptured = true;
		}

		if (!this.minecraft.gui.hud.isHidden()) {
			this.minecraft.gui.hud.toggle();
		}

		if (this.previousBlurriness == null) {
			this.previousBlurriness = this.minecraft.options.getMenuBackgroundBlurriness();
			this.minecraft.options.menuBackgroundBlurriness().set(MAX_BLUR);
		}
	}

	@Override
	public void removed() {
		if (this.hudStateCaptured && this.minecraft != null && this.minecraft.gui != null
				&& !this.hudWasHidden && this.minecraft.gui.hud.isHidden()) {
			this.minecraft.gui.hud.toggle();
		}

		if (this.previousBlurriness != null && this.minecraft != null) {
			this.minecraft.options.menuBackgroundBlurriness().set(this.previousBlurriness);
			this.previousBlurriness = null;
		}

		this.hudStateCaptured = false;

		super.removed();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (this.canUseToggleKey() && KeyBinds.OPEN_GUI.matches(event)) {
			this.onClose();
			return true;
		}

		return super.keyPressed(event);
	}

	/**
	 * Returns whether the interface key may close this screen.
	 *
	 * <p>Subclasses that swallow keyboard input, such as the search field of the
	 * main screen, can opt out while they have focus.</p>
	 *
	 * @return {@code true} when the interface key should close the screen
	 */
	protected boolean canUseToggleKey() {
		return true;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		graphics.blurBeforeThisStratum();

		if (Theme.get().dimBackground()) {
			graphics.fillGradient(0, 0, graphics.guiWidth(), graphics.guiHeight(),
					0xC20B080D, 0xD2080609);
		}
	}
}
