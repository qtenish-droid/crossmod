package io.github.crossmod.client.gui.theme;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The visual identity of CrossMod.
 *
 * <p>The theme is intentionally small and data driven: every colour, radius and
 * opacity used by the interface lives here, so the interface can be restyled
 * without touching a single drawing routine. All values are stored as ARGB
 * integers, which is the format the renderer works with.</p>
 *
 * <p>The surfaces are deliberately translucent. The game stays visible behind
 * the interface: the panel is a sheet of smoked glass, the cards are only a
 * faint tint and every border is a one pixel reflection.</p>
 */
public final class Theme {

	/** The darkening that is laid over the game while an interface is open. */
	public static final int SCRIM = 0x33000000;

	private static Theme instance;

	private final List<Consumer<Theme>> listeners = new ArrayList<>();

	/* ------------------------------------------------------------------ */
	/* Colours                                                             */
	/* ------------------------------------------------------------------ */

	private int accent = 0xFF6FCBFF;
	private int accentSoft = 0xFFA6DFFF;
	private int surfaceTop = 0x3D121823;
	private int surfaceBottom = 0x52101520;
	private int surfaceRaised = 0x260B0E15;
	private int surfaceHover = 0x3D0F141D;
	private int outline = 0x26FFFFFF;
	private int outlineStrong = 0x40FFFFFF;

	private int textPrimary = 0xFFF4F7FC;
	private int textSecondary = 0xFFC3CBD9;
	private int textMuted = 0xFF98A1B2;
	private int textInverse = 0xFF0B0E14;

	private int shadow = 0x59000000;

	/* ------------------------------------------------------------------ */
	/* Metrics                                                             */
	/* ------------------------------------------------------------------ */

	private int cornerRadius = 14;
	private float glassOpacity = 1.0F;
	private int scale = 0;
	private boolean dimBackground = true;
	private boolean showTooltips = true;

	private Theme() {
	}

	/**
	 * Returns the shared theme instance.
	 *
	 * @return the theme, never {@code null}
	 */
	public static Theme get() {
		if (instance == null) {
			instance = new Theme();
		}
		return instance;
	}

	/**
	 * Registers a listener that is invoked whenever the theme changes.
	 *
	 * @param listener the listener to register
	 */
	public void addListener(Consumer<Theme> listener) {
		listeners.add(listener);
	}

	/**
	 * Notifies all listeners that the theme changed.
	 */
	public void markDirty() {
		for (Consumer<Theme> listener : listeners) {
			listener.accept(this);
		}
	}

	public int accent() {
		return accent;
	}

	public void setAccent(int accent) {
		this.accent = accent;
		this.accentSoft = blend(accent, 0xFFFFFFFF, 0.35F);
		markDirty();
	}

	public int accentSoft() {
		return accentSoft;
	}

	public int surfaceTop() {
		return surfaceTop;
	}

	public void setSurfaceTop(int surfaceTop) {
		this.surfaceTop = surfaceTop;
		markDirty();
	}

	public int surfaceBottom() {
		return surfaceBottom;
	}

	public void setSurfaceBottom(int surfaceBottom) {
		this.surfaceBottom = surfaceBottom;
		markDirty();
	}

	public int surfaceRaised() {
		return surfaceRaised;
	}

	public void setSurfaceRaised(int surfaceRaised) {
		this.surfaceRaised = surfaceRaised;
		markDirty();
	}

	public int surfaceHover() {
		return surfaceHover;
	}

	public void setSurfaceHover(int surfaceHover) {
		this.surfaceHover = surfaceHover;
		markDirty();
	}

	public int outline() {
		return outline;
	}

	public void setOutline(int outline) {
		this.outline = outline;
		markDirty();
	}

	public int outlineStrong() {
		return outlineStrong;
	}

	public void setOutlineStrong(int outlineStrong) {
		this.outlineStrong = outlineStrong;
		markDirty();
	}

	public int textPrimary() {
		return textPrimary;
	}

	public void setTextPrimary(int textPrimary) {
		this.textPrimary = textPrimary;
		markDirty();
	}

	public int textSecondary() {
		return textSecondary;
	}

	public void setTextSecondary(int textSecondary) {
		this.textSecondary = textSecondary;
		markDirty();
	}

	public int textMuted() {
		return textMuted;
	}

	public void setTextMuted(int textMuted) {
		this.textMuted = textMuted;
		markDirty();
	}

	public int textInverse() {
		return textInverse;
	}

	public void setTextInverse(int textInverse) {
		this.textInverse = textInverse;
		markDirty();
	}

	public int shadow() {
		return shadow;
	}

	public void setShadow(int shadow) {
		this.shadow = shadow;
		markDirty();
	}

	public int cornerRadius() {
		return cornerRadius;
	}

	public void setCornerRadius(int cornerRadius) {
		this.cornerRadius = Math.max(0, Math.min(32, cornerRadius));
		markDirty();
	}

	public float glassOpacity() {
		return glassOpacity;
	}

	public void setGlassOpacity(float glassOpacity) {
		this.glassOpacity = Math.max(0.15F, Math.min(1.0F, glassOpacity));
		markDirty();
	}

	/**
	 * Returns the requested interface scale.
	 *
	 * @return {@code 0} for automatic, otherwise a factor between 1 and 4
	 */
	public int scale() {
		return scale;
	}

	public void setScale(int scale) {
		this.scale = Math.max(0, Math.min(4, scale));
		markDirty();
	}

	public boolean dimBackground() {
		return dimBackground;
	}

	public void setDimBackground(boolean dimBackground) {
		this.dimBackground = dimBackground;
		markDirty();
	}

	public boolean showTooltips() {
		return showTooltips;
	}

	public void setShowTooltips(boolean showTooltips) {
		this.showTooltips = showTooltips;
		markDirty();
	}

	/**
	 * Restores every value to its factory default.
	 */
	public void reset() {
		accent = 0xFF6FCBFF;
		accentSoft = 0xFFA6DFFF;
		surfaceTop = 0x3D121823;
		surfaceBottom = 0x52101520;
		surfaceRaised = 0x260B0E15;
		surfaceHover = 0x3D0F141D;
		outline = 0x26FFFFFF;
		outlineStrong = 0x40FFFFFF;
		textPrimary = 0xFFF4F7FC;
		textSecondary = 0xFFC3CBD9;
		textMuted = 0xFF98A1B2;
		textInverse = 0xFF0B0E14;
		shadow = 0x59000000;
		cornerRadius = 14;
		glassOpacity = 1.0F;
		scale = 0;
		dimBackground = true;
		showTooltips = true;
		markDirty();
	}

	/**
	 * Blends two ARGB colours.
	 *
	 * @param first  the first colour
	 * @param second the second colour
	 * @param ratio  the weight of the second colour, from 0 to 1
	 * @return the blended colour
	 */
	public static int blend(int first, int second, float ratio) {
		float clamped = Math.max(0.0F, Math.min(1.0F, ratio));

		int alpha = Math.round(alpha(first) * (1.0F - clamped) + alpha(second) * clamped);
		int red = Math.round(red(first) * (1.0F - clamped) + red(second) * clamped);
		int green = Math.round(green(first) * (1.0F - clamped) + green(second) * clamped);
		int blue = Math.round(blue(first) * (1.0F - clamped) + blue(second) * clamped);

		return (alpha << 24) | (red << 16) | (green << 8) | blue;
	}

	public static int alpha(int color) {
		return (color >>> 24) & 0xFF;
	}

	public static int red(int color) {
		return (color >>> 16) & 0xFF;
	}

	public static int green(int color) {
		return (color >>> 8) & 0xFF;
	}

	public static int blue(int color) {
		return color & 0xFF;
	}

	/**
	 * Returns the colour with the given alpha applied.
	 *
	 * @param color the colour
	 * @param alpha the new alpha, from 0 to 255
	 * @return the colour with the replaced alpha
	 */
	public static int withAlpha(int color, int alpha) {
		return (clampByte(alpha) << 24) | (color & 0x00FFFFFF);
	}

	/**
	 * Returns the colour with a multiplied alpha.
	 *
	 * @param color  the colour
	 * @param factor the alpha multiplier
	 * @return the colour with the scaled alpha
	 */
	public static int fade(int color, float factor) {
		return withAlpha(color, Math.round(alpha(color) * factor));
	}

	private static int clampByte(int value) {
		return Math.max(0, Math.min(255, value));
	}
}
