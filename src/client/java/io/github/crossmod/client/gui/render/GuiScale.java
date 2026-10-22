package io.github.crossmod.client.gui.render;

import com.mojang.blaze3d.platform.Window;
import io.github.crossmod.client.gui.theme.Layout;
import net.minecraft.client.Minecraft;

/**
 * Resolution independent scaling of the CrossMod interface.
 *
 * <p>Minecraft renders its interface in a virtual space that is scaled by the
 * GUI scale option. CrossMod deliberately ignores that option: the interface is
 * laid out in its own design space and is then scaled by an integer factor, so
 * it always keeps the same physical size and stays perfectly sharp, no matter
 * which GUI scale the player picked.</p>
 *
 * <p>One Minecraft GUI unit equals {@code guiScale} frame buffer pixels, so a
 * design unit is mapped to the screen with the factor
 * {@code scale / guiScale}.</p>
 */
public final class GuiScale {

	/** The width of the interface in design units. */
	public static final int DESIGN_WIDTH = Layout.DESIGN_WIDTH;

	/** The height of the interface in design units. */
	public static final int DESIGN_HEIGHT = Layout.DESIGN_HEIGHT;

	/** The lowest scale factor that is ever used. */
	public static final int MIN_SCALE = 1;

	/** The highest scale factor that is ever used. */
	public static final int MAX_SCALE = 4;

	/**
	 * The share of the frame buffer width the interface aims to cover. Keeping
	 * this below one prevents the panel from growing edge to edge on large
	 * displays.
	 */
	private static final float TARGET_WIDTH = 0.75F;

	/** The share of the frame buffer height the interface aims to cover. */
	private static final float TARGET_HEIGHT = 0.80F;

	private final int scale;
	private final float designScale;
	private final float originX;
	private final float originY;

	private GuiScale(int scale, float designScale, float originX, float originY) {
		this.scale = scale;
		this.designScale = designScale;
		this.originX = originX;
		this.originY = originY;
	}

	/**
	 * Calculates the scaling for the current window.
	 *
	 * @param requestedScale the requested scale, {@code 0} means automatic
	 * @return the scaling information for the current frame
	 */
	public static GuiScale create(int requestedScale) {
		Minecraft minecraft = Minecraft.getInstance();
		Window window = minecraft.getWindow();

		int framebufferWidth = Math.max(1, window.getWidth());
		int framebufferHeight = Math.max(1, window.getHeight());
		int guiScale = Math.max(1, window.getGuiScale());

		int scale;

		int fittingScale = fittingScale(framebufferWidth, framebufferHeight, 0.96F);

		if (requestedScale > 0) {
			scale = Math.max(MIN_SCALE, Math.min(fittingScale, requestedScale));
		} else {
			float horizontal = framebufferWidth * TARGET_WIDTH / DESIGN_WIDTH;
			float vertical = framebufferHeight * TARGET_HEIGHT / DESIGN_HEIGHT;
			scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, (int) Math.floor(Math.min(horizontal, vertical))));
		}

		float designScale = scale / (float) guiScale;
		float originX = (framebufferWidth - DESIGN_WIDTH * scale) / (2.0F * guiScale);
		float originY = (framebufferHeight - DESIGN_HEIGHT * scale) / (2.0F * guiScale);

		return new GuiScale(scale, designScale, originX, originY);
	}

	/**
	 * Returns the largest scale factor that still fits in the current window.
	 *
	 * @return the largest scale factor that fits with a small margin
	 */
	public static int maxFittingScale() {
		Minecraft minecraft = Minecraft.getInstance();
		Window window = minecraft.getWindow();

		return fittingScale(Math.max(1, window.getWidth()), Math.max(1, window.getHeight()), 0.96F);
	}

	/**
	 * Calculates the largest scale factor that fits the given window size.
	 *
	 * @param width    the frame buffer width
	 * @param height   the frame buffer height
	 * @param coverage the share of the window the interface may cover
	 * @return the largest scale factor
	 */
	private static int fittingScale(int width, int height, float coverage) {
		float horizontal = width * coverage / DESIGN_WIDTH;
		float vertical = height * coverage / DESIGN_HEIGHT;

		return Math.max(MIN_SCALE, Math.min(MAX_SCALE, (int) Math.floor(Math.min(horizontal, vertical))));
	}

	/**
	 * Returns the factor that maps a design unit to a Minecraft GUI unit.
	 *
	 * @return the design scale
	 */
	public float designScale() {
		return designScale;
	}

	/**
	 * Converts a Minecraft GUI coordinate into the design space.
	 *
	 * @param guiCoordinate the coordinate in Minecraft GUI units
	 * @return the coordinate in design units
	 */
	public float toDesignX(float guiCoordinate) {
		return (guiCoordinate - originX) / designScale;
	}

	/**
	 * Converts a Minecraft GUI coordinate into the design space.
	 *
	 * @param guiCoordinate the coordinate in Minecraft GUI units
	 * @return the coordinate in design units
	 */
	public float toDesignY(float guiCoordinate) {
		return (guiCoordinate - originY) / designScale;
	}

	/**
	 * Converts a design coordinate into Minecraft GUI units.
	 *
	 * @param designCoordinate the coordinate in design units
	 * @return the coordinate in Minecraft GUI units
	 */
	public float toGuiX(float designCoordinate) {
		return originX + designCoordinate * designScale;
	}

	/**
	 * Converts a design coordinate into Minecraft GUI units.
	 *
	 * @param designCoordinate the coordinate in design units
	 * @return the coordinate in Minecraft GUI units
	 */
	public float toGuiY(float designCoordinate) {
		return originY + designCoordinate * designScale;
	}
}
