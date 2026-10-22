package io.github.crossmod.client.gui.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Captures the world behind the interface once and turns it into a soft,
 * opaque backdrop.
 *
 * <p>The new screen renderer does not clear translucent GUI layers reliably:
 * compositing the interface directly over the world let old frames accumulate
 * and produced ghosted cards and text. A single low resolution, blurred copy of
 * the world is drawn as an opaque first layer instead. It also gives every
 * CrossMod screen a strong blur without depending on the player's menu blur
 * setting.</p>
 */
public final class MenuBackground {

	private static final Logger LOGGER = LoggerFactory.getLogger("CrossMod/MenuBackground");
	private static final Identifier TEXTURE_ID = Identifier.fromNamespaceAndPath(
			"crossmod", "dynamic/menu_background");
	private static final int TARGET_WIDTH = 160;

	private static volatile boolean ready;
	private static volatile boolean capturing;
	private static volatile int textureWidth = 1;
	private static volatile int textureHeight = 1;

	private MenuBackground() {
	}

	/**
	 * Captures the current frame. Safe to call every time a screen is opened;
	 * only one capture runs at a time.
	 *
	 * @param minecraft the client
	 */
	public static void capture(Minecraft minecraft) {
		if (minecraft == null || capturing) {
			return;
		}

		capturing = true;

		try {
			Screenshot.takeScreenshot(minecraft.gameRenderer.mainRenderTarget(), image -> {
				try {
					store(minecraft, image);
				} catch (RuntimeException exception) {
					LOGGER.warn("Could not build the CrossMod menu background", exception);
					image.close();
				} finally {
					capturing = false;
				}
			});
		} catch (RuntimeException exception) {
			capturing = false;
			LOGGER.warn("Could not capture the CrossMod menu background", exception);
		}
	}

	/**
	 * Draws the backdrop over the whole screen.
	 *
	 * @param graphics the graphics context
	 * @param width    the screen width in interface pixels
	 * @param height   the screen height in interface pixels
	 */
	public static void draw(GuiGraphicsExtractor graphics, int width, int height) {
		if (!ready) {
			graphics.fill(0, 0, width, height, 0xFF05060A);
			return;
		}

		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE_ID, 0, 0, 0.0F, 0.0F,
				width, height, textureWidth, textureHeight, textureWidth, textureHeight, -1);
	}

	private static void store(Minecraft minecraft, NativeImage image) {
		NativeImage blurred = blur(image);
		image.close();

		if (blurred == null) {
			return;
		}

		TextureManager textures = minecraft.getTextureManager();
		AbstractTexture existing = textures.getTexture(TEXTURE_ID);

		// A DynamicTexture cannot be resized in place. The capture height
		// follows the window aspect ratio, so opening the interface after a
		// window resize would otherwise fail with an image size mismatch.
		if (existing instanceof DynamicTexture dynamic
				&& blurred.getWidth() == textureWidth
				&& blurred.getHeight() == textureHeight) {
			dynamic.setPixels(blurred);
			dynamic.upload();
		} else {
			if (existing != null) {
				textures.release(TEXTURE_ID);
			}

			textures.register(TEXTURE_ID, new DynamicTexture(
					() -> "CrossMod menu background", blurred));
		}

		textureWidth = blurred.getWidth();
		textureHeight = blurred.getHeight();
		ready = true;
	}

	private static NativeImage blur(NativeImage source) {
		int sourceWidth = source.getWidth();
		int sourceHeight = source.getHeight();

		if (sourceWidth <= 0 || sourceHeight <= 0) {
			return null;
		}

		int width = Math.min(TARGET_WIDTH, sourceWidth);
		int height = Math.max(1, Math.round((float) width * sourceHeight / sourceWidth));
		int[] sourcePixels = source.getPixelsABGR();
		int[] pixels = new int[width * height];

		for (int y = 0; y < height; y++) {
			int fromY = y * sourceHeight / height;
			int toY = Math.max(fromY + 1, (y + 1) * sourceHeight / height);

			for (int x = 0; x < width; x++) {
				int fromX = x * sourceWidth / width;
				int toX = Math.max(fromX + 1, (x + 1) * sourceWidth / width);
				long alpha = 0L;
				long red = 0L;
				long green = 0L;
				long blue = 0L;
				int count = 0;

				for (int sourceY = fromY; sourceY < toY; sourceY++) {
					int row = sourceY * sourceWidth;

					for (int sourceX = fromX; sourceX < toX; sourceX++) {
						int color = sourcePixels[row + sourceX];
						alpha += (color >>> 24) & 0xFF;
						blue += (color >>> 16) & 0xFF;
						green += (color >>> 8) & 0xFF;
						red += color & 0xFF;
						count++;
					}
				}

				pixels[y * width + x] = (int) ((alpha / count) << 24)
						| (int) ((blue / count) << 16)
						| (int) ((green / count) << 8)
						| (int) (red / count);
			}
		}

		int[] scratch = new int[pixels.length];
		boxBlur(pixels, scratch, width, height, 3);
		boxBlur(scratch, pixels, width, height, 3);

		NativeImage result = new NativeImage(width, height, false);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				result.setPixelABGR(x, y, pixels[y * width + x] | 0xFF000000);
			}
		}

		return result;
	}

	private static void boxBlur(int[] source, int[] target, int width, int height, int radius) {
		int[] horizontal = new int[source.length];

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				long alpha = 0L;
				long red = 0L;
				long green = 0L;
				long blue = 0L;
				int count = 0;

				for (int offset = -radius; offset <= radius; offset++) {
					int sampleX = Math.max(0, Math.min(width - 1, x + offset));
					int color = source[y * width + sampleX];
					alpha += (color >>> 24) & 0xFF;
					blue += (color >>> 16) & 0xFF;
					green += (color >>> 8) & 0xFF;
					red += color & 0xFF;
					count++;
				}

				horizontal[y * width + x] = (int) ((alpha / count) << 24)
						| (int) ((blue / count) << 16)
						| (int) ((green / count) << 8)
						| (int) (red / count);
			}
		}

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				long alpha = 0L;
				long red = 0L;
				long green = 0L;
				long blue = 0L;
				int count = 0;

				for (int offset = -radius; offset <= radius; offset++) {
					int sampleY = Math.max(0, Math.min(height - 1, y + offset));
					int color = horizontal[sampleY * width + x];
					alpha += (color >>> 24) & 0xFF;
					blue += (color >>> 16) & 0xFF;
					green += (color >>> 8) & 0xFF;
					red += color & 0xFF;
					count++;
				}

				target[y * width + x] = (int) ((alpha / count) << 24)
						| (int) ((blue / count) << 16)
						| (int) ((green / count) << 8)
						| (int) (red / count);
			}
		}
	}
}
