package io.github.crossmod.client.gui.render;

import io.github.crossmod.client.gui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Low level drawing helpers shared by every CrossMod interface.
 *
 * <p>All helpers work in the design space of the interface (see
 * {@link GuiScale}), which means every coordinate is resolution independent and
 * ends up on whole pixels.</p>
 *
 * <p>The shapes are translucent by design. They are composited over the blurred
 * game, so a surface never hides what is behind it - it only tints it, the way
 * smoked glass does.</p>
 */
public final class RenderUtils {

	private static final String TEXTURE_DIRECTORY = "textures/gui/";
	private static final int ICON_TEXTURE_SIZE = 128;

	private RenderUtils() {
	}

	/* ------------------------------------------------------------------ */
	/* Shapes                                                              */
	/* ------------------------------------------------------------------ */

	/**
	 * Fills a rectangle with rounded corners.
	 *
	 * @param graphics the graphics context
	 * @param x        the left edge
	 * @param y        the top edge
	 * @param width    the width
	 * @param height   the height
	 * @param radius   the corner radius
	 * @param color    the colour
	 */
	public static void roundedRect(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int radius, int color) {
		roundedRectGradient(graphics, x, y, width, height, radius, color, color);
	}

	/**
	 * Fills a rectangle with rounded corners using a vertical gradient.
	 *
	 * <p>The rectangle is drawn as a stack of horizontal bands. Every band is a
	 * plain axis aligned rectangle whose horizontal inset is derived from the
	 * corner radius, which keeps the edges pixel perfect at every scale.</p>
	 *
	 * @param graphics     the graphics context
	 * @param x            the left edge
	 * @param y            the top edge
	 * @param width        the width
	 * @param height       the height
	 * @param radius       the corner radius
	 * @param topColor     the colour at the top edge
	 * @param bottomColor  the colour at the bottom edge
	 */
	public static void roundedRectGradient(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int radius, int topColor, int bottomColor) {
		if (width <= 0 || height <= 0) {
			return;
		}

		int clampedRadius = Math.max(0, Math.min(radius, Math.min(width, height) / 2));

		if (clampedRadius == 0) {
			if (topColor == bottomColor) {
				graphics.fill(x, y, x + width, y + height, topColor);
			} else {
				graphics.fillGradient(x, y, x + width, y + height, topColor, bottomColor);
			}

			return;
		}

		int bands = Math.max(1, Math.min(512, height));

		for (int band = 0; band < bands; band++) {
			int bandTop = y + band * height / bands;
			int bandBottom = y + (band + 1) * height / bands;
			int bandHeight = bandBottom - bandTop;

			if (bandHeight <= 0) {
				continue;
			}

			float progress = bands == 1 ? 0.0F : (float) band / (bands - 1);
			int color = Theme.blend(topColor, bottomColor, progress);

			int inset = cornerInset(clampedRadius, bandTop - y, bandBottom - y, height);
			graphics.fill(x + inset, bandTop, x + width - inset, bandBottom, color);
		}
	}

	/**
	 * Draws a one pixel outline along the rounded rectangle.
	 *
	 * @param graphics the graphics context
	 * @param x        the left edge
	 * @param y        the top edge
	 * @param width    the width
	 * @param height   the height
	 * @param radius   the corner radius
	 * @param color    the colour of the outline
	 */
	public static void roundedOutline(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int radius, int color) {
		if (width <= 0 || height <= 0) {
			return;
		}

		int clampedRadius = Math.max(0, Math.min(radius, Math.min(width, height) / 2));

		if (clampedRadius == 0) {
			graphics.fill(x, y, x + width, y + 1, color);
			graphics.fill(x, y + height - 1, x + width, y + height, color);
			graphics.fill(x, y, x + 1, y + height, color);
			graphics.fill(x + width - 1, y, x + width, y + height, color);
			return;
		}

		for (int row = 0; row < clampedRadius; row++) {
			int inset = cornerInset(clampedRadius, row, row + 1, height);

			graphics.fill(x + inset, y + row, x + inset + 1, y + row + 1, color);
			graphics.fill(x + width - inset - 1, y + row, x + width - inset, y + row + 1, color);
			graphics.fill(x + inset, y + height - row - 1, x + inset + 1, y + height - row, color);
			graphics.fill(x + width - inset - 1, y + height - row - 1, x + width - inset, y + height - row, color);
		}

		graphics.fill(x + clampedRadius, y, x + width - clampedRadius, y + 1, color);
		graphics.fill(x + clampedRadius, y + height - 1, x + width - clampedRadius, y + height, color);
		graphics.fill(x, y + clampedRadius, x + 1, y + height - clampedRadius, color);
		graphics.fill(x + width - 1, y + clampedRadius, x + width, y + height - clampedRadius, color);
	}

	/**
	 * Draws a soft drop shadow underneath a rounded rectangle.
	 *
	 * @param graphics the graphics context
	 * @param x        the left edge
	 * @param y        the top edge
	 * @param width    the width
	 * @param height   the height
	 * @param radius   the corner radius
	 * @param spread   how far the shadow extends, in design pixels
	 * @param alpha    the alpha of the innermost shadow layer
	 */
	public static void dropShadow(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int radius, int spread, int alpha) {
		int layers = Math.max(1, spread);

		for (int layer = layers; layer >= 1; layer--) {
			float strength = 1.0F - (float) (layer - 1) / layers;
			int layerAlpha = Math.round(alpha * strength * strength / layers * 3.0F);

			if (layerAlpha <= 0) {
				continue;
			}

			roundedRect(graphics, x - layer, y - layer + 2, width + layer * 2, height + layer * 2, radius + layer,
					Theme.withAlpha(0xFF000000, layerAlpha));
		}
	}

	/**
	 * Draws the glass surface used for every panel of the interface.
	 *
	 * <p>The surface is a vertical gradient of two translucent dark tones. Its
	 * alpha is multiplied by the glass opacity that the player picked in the
	 * interface settings.</p>
	 *
	 * @param graphics the graphics context
	 * @param x        the left edge
	 * @param y        the top edge
	 * @param width    the width
	 * @param height   the height
	 * @param radius   the corner radius
	 * @param alpha    an extra transparency factor
	 */
	public static void glassPanel(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int radius, float alpha) {
		Theme theme = Theme.get();

		int top = Theme.withAlpha(theme.surfaceTop(), Math.round(Theme.alpha(theme.surfaceTop()) * theme.glassOpacity() * alpha));
		int bottom = Theme.withAlpha(theme.surfaceBottom(), Math.round(Theme.alpha(theme.surfaceBottom()) * theme.glassOpacity() * alpha));

		roundedRectGradient(graphics, x, y, width, height, radius, top, bottom);
	}

	/**
	 * Draws the soft reflection across the upper part of a glass surface.
	 *
	 * @param graphics the graphics context
	 * @param x        the left edge
	 * @param y        the top edge
	 * @param width    the width
	 * @param height   the height
	 * @param radius   the corner radius
	 */
	public static void glassSheen(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int radius) {
		int sheenHeight = Math.max(1, height * 2 / 5);

		roundedRectGradient(graphics, x + 1, y + 1, width - 2, sheenHeight, Math.max(0, radius - 1),
				0x14FFFFFF, 0x00FFFFFF);
	}

	/**
	 * Draws the specular highlight of a glass surface.
	 *
	 * <p>A bright line along the top edge and a softer second line below it make
	 * a flat panel read as a piece of frosted glass.</p>
	 *
	 * @param graphics the graphics context
	 * @param x        the left edge
	 * @param y        the top edge
	 * @param width    the width
	 * @param height   the height
	 * @param radius   the corner radius
	 */
	public static void glassHighlight(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int radius) {
		int clampedRadius = Math.max(0, Math.min(radius, Math.min(width, height) / 2));

		graphics.fill(x + clampedRadius, y, x + width - clampedRadius, y + 1, 0x4DFFFFFF);
		graphics.fill(x + clampedRadius, y + 1, x + width - clampedRadius, y + 2, 0x17FFFFFF);
	}

	/**
	 * Draws a small toggle switch.
	 *
	 * @param graphics the graphics context
	 * @param x        the left edge
	 * @param y        the top edge
	 * @param width    the width of the switch
	 * @param height   the height of the switch
	 * @param active   whether the switch is switched on
	 * @param accent   the accent colour
	 */
	public static void toggleSwitch(GuiGraphicsExtractor graphics, int x, int y, int width, int height, boolean active, int accent) {
		int background = active ? accent : 0x59000000;
		roundedRect(graphics, x, y, width, height, height / 2, background);
		roundedOutline(graphics, x, y, width, height, height / 2,
				active ? Theme.withAlpha(0xFFFFFFFF, 0x40) : Theme.withAlpha(0xFFFFFFFF, 0x1F));

		int knobRadius = height / 2 - 2;
		int knobCentreX = active ? x + width - knobRadius - 2 : x + knobRadius + 2;
		int knobColour = active ? 0xFFFFFFFF : 0xFFD5DCE7;

		roundedRect(graphics, knobCentreX - knobRadius, y + 2, knobRadius * 2, knobRadius * 2, knobRadius, knobColour);
	}

	/**
	 * Draws a horizontal progress bar.
	 *
	 * @param graphics the graphics context
	 * @param x        the left edge
	 * @param y        the top edge
	 * @param width    the width
	 * @param height   the height
	 * @param progress the fill between 0 and 1
	 * @param accent   the accent colour
	 */
	public static void progressBar(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float progress, int accent) {
		roundedRect(graphics, x, y, width, height, height, 0x59000000);

		int fill = Math.round(width * Math.max(0.0F, Math.min(1.0F, progress)));

		if (fill > 0) {
			roundedRect(graphics, x, y, fill, height, height, accent);
		}

		roundedOutline(graphics, x, y, width, height, height, 0x26FFFFFF);
	}

	/* ------------------------------------------------------------------ */
	/* Text                                                                */
	/* ------------------------------------------------------------------ */

	/**
	 * Truncates a string so that it never exceeds the given width.
	 *
	 * @param font     the font used for measuring
	 * @param text     the text
	 * @param maxWidth the maximum width in pixels
	 * @return the truncated text
	 */
	public static String trim(Font font, String text, int maxWidth) {
		if (text == null) {
			return "";
		}

		if (font.width(text) <= maxWidth) {
			return text;
		}

		String trimmed = font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("...")));
		return trimmed + "...";
	}

	/* ------------------------------------------------------------------ */
	/* Textures                                                            */
	/* ------------------------------------------------------------------ */

	/**
	 * Resolves a texture inside the CrossMod asset directory.
	 *
	 * @param name the name of the texture, for example {@code gear.png}
	 * @return the texture location
	 */
	public static Identifier texture(String name) {
		return Identifier.fromNamespaceAndPath("crossmod", TEXTURE_DIRECTORY + name);
	}

	/**
	 * Draws an icon texture.
	 *
	 * @param graphics the graphics context
	 * @param name     the name of the texture
	 * @param x        the left edge
	 * @param y        the top edge
	 * @param size     the size of the icon
	 */
	public static void icon(GuiGraphicsExtractor graphics, String name, int x, int y, int size) {
		icon(graphics, name, x, y, size, 0xFFFFFFFF);
	}

	/**
	 * Draws a tinted icon texture.
	 *
	 * @param graphics the graphics context
	 * @param name     the name of the texture
	 * @param x        the left edge
	 * @param y        the top edge
	 * @param size     the size of the icon
	 * @param color    the tint of the icon
	 */
	public static void icon(GuiGraphicsExtractor graphics, String name, int x, int y, int size, int color) {
		texture(graphics, name, x, y, size, size, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE, color);
	}

	/**
	 * Draws a whole texture at an arbitrary size.
	 *
	 * @param graphics      the graphics context
	 * @param name          the texture name
	 * @param x             the left edge
	 * @param y             the top edge
	 * @param width         the drawn width
	 * @param height        the drawn height
	 * @param textureWidth  the width of the source image
	 * @param textureHeight the height of the source image
	 * @param color         the tint
	 */
	public static void texture(GuiGraphicsExtractor graphics, String name, int x, int y,
			int width, int height, int textureWidth, int textureHeight, int color) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture(name), x, y, 0.0F, 0.0F, width, height,
				textureWidth, textureHeight, textureWidth, textureHeight, color);
	}

	/**
	 * Draws a horizontally sliced surface.
	 *
	 * <p>The left and right caps keep their painted width while the middle is
	 * stretched. The height is always mapped one to one, which preserves the
	 * vertical gradient and the one pixel border of the surface.</p>
	 *
	 * @param graphics      the graphics context
	 * @param name          the texture name
	 * @param x             the left edge
	 * @param y             the top edge
	 * @param width         the drawn width
	 * @param height        the drawn height
	 * @param textureWidth  the width of the source image
	 * @param textureHeight the height of the source image
	 * @param sourceCap     the cap width in source pixels
	 * @param destinationCap the cap width in interface pixels
	 * @param color         the tint
	 */
	public static void horizontalSlice(GuiGraphicsExtractor graphics, String name, int x, int y,
			int width, int height, int textureWidth, int textureHeight,
			int sourceCap, int destinationCap, int color) {
		horizontalSlice(graphics, name, x, y, width, height, textureWidth, textureHeight,
				sourceCap, sourceCap, destinationCap, destinationCap, color);
	}

	/**
	 * Draws a horizontally sliced surface with independent caps.
	 *
	 * @param graphics       the graphics context
	 * @param name           the texture name
	 * @param x              the left edge
	 * @param y              the top edge
	 * @param width          the drawn width
	 * @param height         the drawn height
	 * @param textureWidth   the width of the source image
	 * @param textureHeight  the height of the source image
	 * @param sourceLeft     the left cap in source pixels
	 * @param sourceRight    the right cap in source pixels
	 * @param destinationLeft the left cap in interface pixels
	 * @param destinationRight the right cap in interface pixels
	 * @param color          the tint
	 */
	public static void horizontalSlice(GuiGraphicsExtractor graphics, String name, int x, int y,
			int width, int height, int textureWidth, int textureHeight,
			int sourceLeft, int sourceRight, int destinationLeft, int destinationRight, int color) {
		if (width <= 0 || height <= 0) {
			return;
		}

		Identifier texture = texture(name);
		int middleWidth = width - destinationLeft - destinationRight;

		if (middleWidth <= 0 || sourceLeft + sourceRight >= textureWidth) {
			texture(graphics, name, x, y, width, height, textureWidth, textureHeight, color);
			return;
		}

		if (destinationLeft > 0 && sourceLeft > 0) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F,
					destinationLeft, height, sourceLeft, textureHeight, textureWidth, textureHeight, color);
		}

		int sourceMiddle = textureWidth - sourceLeft - sourceRight;
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + destinationLeft, y,
				(float) sourceLeft, 0.0F, middleWidth, height,
				sourceMiddle, textureHeight, textureWidth, textureHeight, color);

		if (destinationRight > 0 && sourceRight > 0) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, texture,
					x + width - destinationRight, y, (float) (textureWidth - sourceRight), 0.0F,
					destinationRight, height, sourceRight, textureHeight, textureWidth, textureHeight, color);
		}
	}

	/**
	 * Draws a nine sliced surface.
	 *
	 * @param graphics      the graphics context
	 * @param name          the texture name
	 * @param x             the left edge
	 * @param y             the top edge
	 * @param width         the drawn width
	 * @param height        the drawn height
	 * @param textureWidth  the width of the source image
	 * @param textureHeight the height of the source image
	 * @param sourceCap     the cap size in source pixels
	 * @param destinationCap the cap size in interface pixels
	 * @param color         the tint
	 */
	public static void nineSlice(GuiGraphicsExtractor graphics, String name, int x, int y,
			int width, int height, int textureWidth, int textureHeight,
			int sourceCap, int destinationCap, int color) {
		if (width <= 0 || height <= 0) {
			return;
		}

		if (width <= destinationCap * 2 || height <= destinationCap * 2) {
			texture(graphics, name, x, y, width, height, textureWidth, textureHeight, color);
			return;
		}

		Identifier texture = texture(name);
		int middleWidth = width - destinationCap * 2;
		int middleHeight = height - destinationCap * 2;
		int sourceMiddleWidth = textureWidth - sourceCap * 2;
		int sourceMiddleHeight = textureHeight - sourceCap * 2;
		float rightU = (float) (textureWidth - sourceCap);
		float bottomV = (float) (textureHeight - sourceCap);

		blitRegion(graphics, texture, x, y, 0.0F, 0.0F,
				destinationCap, destinationCap, textureWidth, textureHeight, sourceCap, sourceCap, color);
		blitRegion(graphics, texture, x + destinationCap, y, (float) sourceCap, 0.0F,
				middleWidth, destinationCap, textureWidth, textureHeight, sourceMiddleWidth, sourceCap, color);
		blitRegion(graphics, texture, x + width - destinationCap, y, rightU, 0.0F,
				destinationCap, destinationCap, textureWidth, textureHeight, sourceCap, sourceCap, color);

		blitRegion(graphics, texture, x, y + destinationCap, 0.0F, (float) sourceCap,
				destinationCap, middleHeight, textureWidth, textureHeight, sourceCap, sourceMiddleHeight, color);
		blitRegion(graphics, texture, x + destinationCap, y + destinationCap, (float) sourceCap, (float) sourceCap,
				middleWidth, middleHeight, textureWidth, textureHeight, sourceMiddleWidth, sourceMiddleHeight, color);
		blitRegion(graphics, texture, x + width - destinationCap, y + destinationCap, rightU, (float) sourceCap,
				destinationCap, middleHeight, textureWidth, textureHeight, sourceCap, sourceMiddleHeight, color);

		blitRegion(graphics, texture, x, y + height - destinationCap, 0.0F, bottomV,
				destinationCap, destinationCap, textureWidth, textureHeight, sourceCap, sourceCap, color);
		blitRegion(graphics, texture, x + destinationCap, y + height - destinationCap, (float) sourceCap, bottomV,
				middleWidth, destinationCap, textureWidth, textureHeight, sourceMiddleWidth, sourceCap, color);
		blitRegion(graphics, texture, x + width - destinationCap, y + height - destinationCap, rightU, bottomV,
				destinationCap, destinationCap, textureWidth, textureHeight, sourceCap, sourceCap, color);
	}

	private static void blitRegion(GuiGraphicsExtractor graphics, Identifier texture,
			int x, int y, float u, float v, int width, int height,
			int textureWidth, int textureHeight, int sourceWidth, int sourceHeight, int color) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height,
				sourceWidth, sourceHeight, textureWidth, textureHeight, color);
	}

	/* ------------------------------------------------------------------ */
	/* Internals                                                           */
	/* ------------------------------------------------------------------ */

	/**
	 * Calculates the horizontal inset of a rounded corner for a band of rows.
	 *
	 * @param radius     the corner radius
	 * @param bandTop    the top of the band, relative to the rectangle
	 * @param bandBottom the bottom of the band, relative to the rectangle
	 * @param height     the total height of the rectangle
	 * @return the inset in pixels
	 */
	private static int cornerInset(int radius, int bandTop, int bandBottom, int height) {
		int distance = Math.min(bandTop, height - bandBottom);

		if (distance >= radius) {
			return 0;
		}

		double squared = (double) radius * radius - (double) (radius - distance) * (radius - distance);
		int inset = radius - (int) Math.round(Math.sqrt(Math.max(0.0D, squared)));

		return Math.max(0, Math.min(radius, inset));
	}
}
