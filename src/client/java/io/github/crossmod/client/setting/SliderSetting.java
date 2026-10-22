package io.github.crossmod.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * A setting that holds a number inside a range.
 */
public final class SliderSetting extends Setting<Double> {

	private final double minimum;
	private final double maximum;
	private final double step;

	public SliderSetting(String id, String name, String description, double defaultValue, double minimum, double maximum, double step) {
		super(id, name, description, defaultValue);
		this.minimum = minimum;
		this.maximum = maximum;
		this.step = step;
	}

	public double minimum() {
		return minimum;
	}

	public double maximum() {
		return maximum;
	}

	public double step() {
		return step;
	}

	/**
	 * Returns the position of the current value inside the range, from 0 to 1.
	 *
	 * @return the normalised value
	 */
	public double progress() {
		if (maximum <= minimum) {
			return 0.0D;
		}

		return Math.max(0.0D, Math.min(1.0D, (value() - minimum) / (maximum - minimum)));
	}

	/**
	 * Sets the value from a normalised position, from 0 to 1.
	 *
	 * @param progress the normalised position
	 */
	public void setProgress(double progress) {
		double clamped = Math.max(0.0D, Math.min(1.0D, progress));
		setValue(round(minimum + clamped * (maximum - minimum)));
	}

	/**
	 * Moves the value by the given number of steps.
	 *
	 * @param steps the number of steps, may be negative
	 */
	public void step(int steps) {
		double next = value() + step * steps;
		setValue(Math.max(minimum, Math.min(maximum, round(next))));
	}

	private double round(double value) {
		double scaled = value / step;
		return Math.round(scaled) * step;
	}

	@Override
	public JsonElement write() {
		return new JsonPrimitive(value());
	}

	@Override
	public void read(JsonElement element) {
		if (element != null && element.isJsonPrimitive()) {
			double stored = element.getAsDouble();
			setValue(Math.max(minimum, Math.min(maximum, round(stored))));
		}
	}
}
