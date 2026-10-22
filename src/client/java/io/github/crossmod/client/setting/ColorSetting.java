package io.github.crossmod.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * A setting that holds a colour in the ARGB format.
 */
public final class ColorSetting extends Setting<Integer> {

	public ColorSetting(String id, String name, String description, int defaultValue) {
		super(id, name, description, defaultValue);
	}

	/**
	 * Returns the hue of the colour, from 0 to 1.
	 *
	 * @return the hue
	 */
	public float hue() {
		return hue(value());
	}

	/**
	 * Returns the saturation of the colour, from 0 to 1.
	 *
	 * @return the saturation
	 */
	public float saturation() {
		return saturation(value());
	}

	/**
	 * Returns the brightness of the colour, from 0 to 1.
	 *
	 * @return the brightness
	 */
	public float brightness() {
		return brightness(value());
	}

	/**
	 * Returns a new colour with the given hue, keeping saturation and brightness.
	 *
	 * @param hue the new hue, from 0 to 1
	 * @return the new colour
	 */
	public int withHue(float hue) {
		return fromHsv(hue, saturation(), brightness());
	}

	/**
	 * Returns a new colour with the given saturation, keeping hue and brightness.
	 *
	 * @param saturation the new saturation, from 0 to 1
	 * @return the new colour
	 */
	public int withSaturation(float saturation) {
		return fromHsv(hue(), saturation, brightness());
	}

	/**
	 * Returns a new colour with the given brightness, keeping hue and saturation.
	 *
	 * @param brightness the new brightness, from 0 to 1
	 * @return the new colour
	 */
	public int withBrightness(float brightness) {
		return fromHsv(hue(), saturation(), brightness);
	}

	private static float hue(int color) {
		float[] hsv = toHsv(color);
		return hsv[0];
	}

	private static float saturation(int color) {
		float[] hsv = toHsv(color);
		return hsv[1];
	}

	private static float brightness(int color) {
		float[] hsv = toHsv(color);
		return hsv[2];
	}

	private static float[] toHsv(int color) {
		float red = ((color >>> 16) & 0xFF) / 255.0F;
		float green = ((color >>> 8) & 0xFF) / 255.0F;
		float blue = (color & 0xFF) / 255.0F;

		float max = Math.max(red, Math.max(green, blue));
		float min = Math.min(red, Math.min(green, blue));
		float delta = max - min;

		float hue = 0.0F;

		if (delta > 0.0F) {
			if (max == red) {
				hue = ((green - blue) / delta) % 6.0F;
			} else if (max == green) {
				hue = (blue - red) / delta + 2.0F;
			} else {
				hue = (red - green) / delta + 4.0F;
			}

			hue /= 6.0F;

			if (hue < 0.0F) {
				hue += 1.0F;
			}
		}

		float saturation = max <= 0.0F ? 0.0F : delta / max;

		return new float[]{hue, saturation, max};
	}

	private int fromHsv(float hue, float saturation, float brightness) {
		int red = Math.round(brightness * 255.0F);
		int green = Math.round(brightness * 255.0F);
		int blue = Math.round(brightness * 255.0F);

		if (saturation > 0.0F) {
			float h = (hue % 1.0F + 1.0F) % 1.0F * 6.0F;
			int sector = (int) Math.floor(h);
			float fraction = h - sector;
			float p = brightness * (1.0F - saturation);
			float q = brightness * (1.0F - saturation * fraction);
			float t = brightness * (1.0F - saturation * (1.0F - fraction));

			switch (sector) {
				case 0 -> {
					red = Math.round(brightness * 255.0F);
					green = Math.round(t * 255.0F);
					blue = Math.round(p * 255.0F);
				}
				case 1 -> {
					red = Math.round(q * 255.0F);
					green = Math.round(brightness * 255.0F);
					blue = Math.round(p * 255.0F);
				}
				case 2 -> {
					red = Math.round(p * 255.0F);
					green = Math.round(brightness * 255.0F);
					blue = Math.round(t * 255.0F);
				}
				case 3 -> {
					red = Math.round(p * 255.0F);
					green = Math.round(q * 255.0F);
					blue = Math.round(brightness * 255.0F);
				}
				case 4 -> {
					red = Math.round(t * 255.0F);
					green = Math.round(p * 255.0F);
					blue = Math.round(brightness * 255.0F);
				}
				default -> {
					red = Math.round(brightness * 255.0F);
					green = Math.round(p * 255.0F);
					blue = Math.round(q * 255.0F);
				}
			}
		}

		int alpha = (this.value() >>> 24) & 0xFF;

		return (alpha << 24) | (red << 16) | (green << 8) | blue;
	}

	@Override
	public JsonElement write() {
		return new JsonPrimitive(value());
	}

	@Override
	public void read(JsonElement element) {
		if (element != null && element.isJsonPrimitive()) {
			setValue(element.getAsInt());
		}
	}
}
