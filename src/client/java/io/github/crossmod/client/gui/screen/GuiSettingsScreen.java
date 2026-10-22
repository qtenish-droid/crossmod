package io.github.crossmod.client.gui.screen;

import io.github.crossmod.client.gui.render.GuiScale;
import io.github.crossmod.client.gui.render.RenderUtils;
import io.github.crossmod.client.gui.theme.Theme;
import io.github.crossmod.client.module.ModuleManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The settings of the interface itself.
 *
 * <p>Reached through the gear in the sidebar. Everything that is changed here
 * is written to the configuration file, so the interface looks the same the
 * next time the game is started.</p>
 */
public final class GuiSettingsScreen extends GlassScreen {

	private static final int ROW_HEIGHT = 30;
	private static final int CONTROL_WIDTH = 130;
	private static final int CONTROL_HEIGHT = 18;
	private static final int PANEL_WIDTH = 470;

	private final ModuleManager moduleManager;
	private final List<Row> rows = new ArrayList<>();

	private GuiScale guiScale = GuiScale.create(0);
	private float mouseX;
	private float mouseY;

	public GuiSettingsScreen(ModuleManager moduleManager) {
		super(Component.literal("Interface settings"));
		this.moduleManager = moduleManager;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		this.guiScale = GuiScale.create(Theme.get().scale());
		this.mouseX = this.guiScale.toDesignX(mouseX);
		this.mouseY = this.guiScale.toDesignY(mouseY);
		this.rows.clear();

		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(this.guiScale.toGuiX(0.0F), this.guiScale.toGuiY(0.0F));
		pose.scale(this.guiScale.designScale(), this.guiScale.designScale());

		renderPanel(graphics);

		pose.popMatrix();
	}

	/**
	 * Draws the panel with the header and the settings.
	 *
	 * @param graphics the graphics context
	 */
	private void renderPanel(GuiGraphicsExtractor graphics) {
		Theme theme = Theme.get();
		int radius = theme.cornerRadius();

		int height = 74 + 6 * ROW_HEIGHT + 14;
		int x = (GuiScale.DESIGN_WIDTH - PANEL_WIDTH) / 2;
		int y = Math.max(10, (GuiScale.DESIGN_HEIGHT - height) / 2);

		RenderUtils.dropShadow(graphics, x, y, PANEL_WIDTH, height, radius, 8, 0x66);
		RenderUtils.glassPanel(graphics, x, y, PANEL_WIDTH, height, radius, 1.0F);
		RenderUtils.glassSheen(graphics, x, y, PANEL_WIDTH, height, radius);
		RenderUtils.roundedOutline(graphics, x, y, PANEL_WIDTH, height, radius, theme.outline());
		RenderUtils.glassHighlight(graphics, x, y, PANEL_WIDTH, height, radius);

		RenderUtils.icon(graphics, "back.png", x + 14, y + 14, 16, Theme.withAlpha(theme.textSecondary(), 0xFF));
		graphics.text(this.font, "Interface", x + 38, y + 16, theme.textPrimary(), false);
		graphics.text(this.font, "Appearance of the CrossMod interface", x + 38, y + 28,
				Theme.withAlpha(theme.textMuted(), 0xFF), false);

		this.rows.add(new Row(x + 8, y + 10, 24, 24, RowType.BACK, null));

		int rowY = y + 54;
		int rowX = x + 14;
		int rowWidth = PANEL_WIDTH - 28;

		renderScaleRow(graphics, rowX, rowY, rowWidth);
		rowY += ROW_HEIGHT;

		renderAccentRow(graphics, rowX, rowY, rowWidth);
		rowY += ROW_HEIGHT;

		renderOpacityRow(graphics, rowX, rowY, rowWidth);
		rowY += ROW_HEIGHT;

		renderRadiusRow(graphics, rowX, rowY, rowWidth);
		rowY += ROW_HEIGHT;

		renderToggleRow(graphics, "Dim the background", "Darkens the game behind the interface", rowX, rowY, rowWidth,
				theme.dimBackground(), value -> theme.setDimBackground(value), theme::dimBackground);
		rowY += ROW_HEIGHT;

		renderToggleRow(graphics, "Show tooltips", "Shows a tooltip when hovering an element", rowX, rowY, rowWidth,
				theme.showTooltips(), value -> theme.setShowTooltips(value), theme::showTooltips);
	}

	/**
	 * Draws the row that selects the interface scale.
	 *
	 * @param graphics the graphics context
	 * @param x        the left edge
	 * @param y        the top edge
	 * @param width    the width of the row
	 */
	private void renderScaleRow(GuiGraphicsExtractor graphics, int x, int y, int width) {
		Theme theme = Theme.get();
		int maxScale = GuiScale.maxFittingScale();
		List<String> options = new ArrayList<>(maxScale + 1);
		options.add("Auto");

		for (int scale = 1; scale <= maxScale; scale++) {
			options.add(scale + "x");
		}

		int index = Math.max(0, Math.min(maxScale, theme.scale()));

		graphics.text(this.font, "Interface scale", x, y + 6, theme.textPrimary(), false);

		int controlX = x + width - CONTROL_WIDTH;
		boolean hovered = isHovered(controlX, y, CONTROL_WIDTH, CONTROL_HEIGHT);

		RenderUtils.roundedRect(graphics, controlX, y, CONTROL_WIDTH, CONTROL_HEIGHT, 6,
				hovered ? Theme.withAlpha(theme.accent(), 0x26) : 0x33000000);
		RenderUtils.roundedOutline(graphics, controlX, y, CONTROL_WIDTH, CONTROL_HEIGHT, 6,
				hovered ? Theme.withAlpha(theme.accent(), 0x99) : theme.outline());

		String value = options.get(index);
		graphics.text(this.font, value, controlX + 8, y + 5, theme.textPrimary(), false);
		RenderUtils.icon(graphics, "chevron_right.png", controlX + CONTROL_WIDTH - 16, y + 3, 12,
				Theme.withAlpha(theme.textSecondary(), 0xFF));

		this.rows.add(new Row(controlX, y, CONTROL_WIDTH, CONTROL_HEIGHT, RowType.SCALE, null));
	}

	/**
	 * Draws the row that selects the accent colour.
	 *
	 * @param graphics the graphics context
	 * @param x        the left edge
	 * @param y        the top edge
	 * @param width    the width of the row
	 */
	private void renderAccentRow(GuiGraphicsExtractor graphics, int x, int y, int width) {
		Theme theme = Theme.get();

		graphics.text(this.font, "Accent colour", x, y + 6, theme.textPrimary(), false);

		int controlX = x + width - CONTROL_WIDTH;
		boolean hovered = isHovered(controlX, y, CONTROL_WIDTH, CONTROL_HEIGHT);

		RenderUtils.roundedRect(graphics, controlX, y, CONTROL_WIDTH, CONTROL_HEIGHT, 6,
				hovered ? Theme.withAlpha(theme.accent(), 0x26) : 0x33000000);
		RenderUtils.roundedOutline(graphics, controlX, y, CONTROL_WIDTH, CONTROL_HEIGHT, 6,
				hovered ? Theme.withAlpha(theme.accent(), 0x99) : theme.outline());

		RenderUtils.roundedRect(graphics, controlX + 6, y + 4, 10, 10, 3, theme.accent());
		RenderUtils.roundedOutline(graphics, controlX + 6, y + 4, 10, 10, 3, theme.outlineStrong());

		graphics.text(this.font, String.format("#%06X", theme.accent() & 0xFFFFFF), controlX + 22, y + 5,
				theme.textPrimary(), false);

		this.rows.add(new Row(controlX, y, CONTROL_WIDTH, CONTROL_HEIGHT, RowType.ACCENT, null));
	}

	/**
	 * Draws the row that controls the opacity of the glass.
	 *
	 * @param graphics the graphics context
	 * @param x        the left edge
	 * @param y        the top edge
	 * @param width    the width of the row
	 */
	private void renderOpacityRow(GuiGraphicsExtractor graphics, int x, int y, int width) {
		Theme theme = Theme.get();

		graphics.text(this.font, "Glass opacity", x, y + 6, theme.textPrimary(), false);

		int controlX = x + width - CONTROL_WIDTH;
		RenderUtils.progressBar(graphics, controlX, y + 7, CONTROL_WIDTH, 6, theme.glassOpacity(), theme.accent());

		String value = Math.round(theme.glassOpacity() * 100.0F) + "%";
		graphics.text(this.font, value, controlX + CONTROL_WIDTH - this.font.width(value), y - 2,
				Theme.withAlpha(theme.textSecondary(), 0xFF), false);

		this.rows.add(new Row(controlX, y, CONTROL_WIDTH, CONTROL_HEIGHT, RowType.OPACITY, null));
	}

	/**
	 * Draws the row that controls the corner radius.
	 *
	 * @param graphics the graphics context
	 * @param x        the left edge
	 * @param y        the top edge
	 * @param width    the width of the row
	 */
	private void renderRadiusRow(GuiGraphicsExtractor graphics, int x, int y, int width) {
		Theme theme = Theme.get();

		graphics.text(this.font, "Corner radius", x, y + 6, theme.textPrimary(), false);

		int controlX = x + width - CONTROL_WIDTH;
		RenderUtils.progressBar(graphics, controlX, y + 7, CONTROL_WIDTH, 6, theme.cornerRadius() / 24.0F, theme.accent());

		String value = theme.cornerRadius() + "px";
		graphics.text(this.font, value, controlX + CONTROL_WIDTH - this.font.width(value), y - 2,
				Theme.withAlpha(theme.textSecondary(), 0xFF), false);

		this.rows.add(new Row(controlX, y, CONTROL_WIDTH, CONTROL_HEIGHT, RowType.RADIUS, null));
	}

	/**
	 * Draws a row with a switch.
	 *
	 * @param graphics  the graphics context
	 * @param name      the name of the setting
	 * @param description the description of the setting
	 * @param x         the left edge
	 * @param y         the top edge
	 * @param width     the width of the row
	 * @param value     the current value
	 * @param onChange  called with the new value
	 */
	private void renderToggleRow(GuiGraphicsExtractor graphics, String name, String description, int x, int y, int width,
			boolean value, java.util.function.Consumer<Boolean> onChange, java.util.function.BooleanSupplier currentValue) {
		Theme theme = Theme.get();

		graphics.text(this.font, name, x, y + 2, theme.textPrimary(), false);
		graphics.text(this.font, description, x, y + 14, Theme.withAlpha(theme.textMuted(), 0xFF), false);

		int controlX = x + width - 22;
		RenderUtils.toggleSwitch(graphics, controlX, y + 6, 22, 12, value, theme.accent());

		this.rows.add(new Row(controlX, y, 22, 18, RowType.TOGGLE, new ToggleControl(onChange, currentValue)));
	}

	/* ------------------------------------------------------------------ */
	/* Interaction                                                          */
	/* ------------------------------------------------------------------ */

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		float x = this.guiScale.toDesignX((float) event.x());
		float y = this.guiScale.toDesignY((float) event.y());

		for (int index = this.rows.size() - 1; index >= 0; index--) {
			Row row = this.rows.get(index);

			if (!row.contains(x, y)) {
				continue;
			}

			Theme theme = Theme.get();

			switch (row.type()) {
				case BACK -> {
					this.minecraft.setScreenAndShow(new CrossModScreen(this.moduleManager));
					return true;
				}
				case SCALE -> {
					int maxScale = GuiScale.maxFittingScale();
					theme.setScale((Math.min(theme.scale(), maxScale) + 1) % (maxScale + 1));
					this.guiScale = GuiScale.create(theme.scale());
					return true;
				}
				case ACCENT -> {
					theme.setAccent(nextAccent(theme.accent()));
					return true;
				}
				case TOGGLE -> {
					ToggleControl control = (ToggleControl) row.payload();
					control.onChange().accept(!control.currentValue().getAsBoolean());
					return true;
				}
				default -> {
					return true;
				}
			}
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		float x = this.guiScale.toDesignX((float) event.x());
		float y = this.guiScale.toDesignY((float) event.y());

		for (Row row : this.rows) {
			if (!row.contains(x, y)) {
				continue;
			}

			Theme theme = Theme.get();

			switch (row.type()) {
				case OPACITY -> {
					theme.setGlassOpacity((x - row.x()) / (float) CONTROL_WIDTH);
					return true;
				}
				case RADIUS -> {
					theme.setCornerRadius(Math.round((x - row.x()) / (float) CONTROL_WIDTH * 24.0F));
					return true;
				}
				default -> {
					return true;
				}
			}
		}

		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreenAndShow(new CrossModScreen(this.moduleManager));
	}

	/* ------------------------------------------------------------------ */
	/* Helpers                                                             */
	/* ------------------------------------------------------------------ */

	/**
	 * Returns the next accent colour of the colour wheel.
	 *
	 * @param accent the current accent colour
	 * @return the next accent colour
	 */
	private int nextAccent(int accent) {
		int[] palette = {0xFF5CC8FF, 0xFF7CFFB2, 0xFFFFD166, 0xFFFF7B9C, 0xFFB08CFF, 0xFFFFFFFF};
		int current = palette[0];

		for (int colour : palette) {
			if (colour == accent) {
				current = colour;
				break;
			}
		}

		for (int index = 0; index < palette.length; index++) {
			if (palette[index] == current) {
				return palette[(index + 1) % palette.length];
			}
		}

		return palette[0];
	}

	private boolean isHovered(float x, float y, int width, int height) {
		return this.mouseX >= x && this.mouseX < x + width && this.mouseY >= y && this.mouseY < y + height;
	}

	/**
	 * The kind of an interactive row.
	 */
	private enum RowType {
		BACK,
		SCALE,
		ACCENT,
		OPACITY,
		RADIUS,
		TOGGLE
	}

	/**
	 * A switch that writes back into the theme.
	 *
	 * @param onChange    called with the new value
	 * @param currentValue returns the current value
	 */
	private record ToggleControl(java.util.function.Consumer<Boolean> onChange, java.util.function.BooleanSupplier currentValue) {
	}

	/**
	 * An interactive row of the settings screen.
	 *
	 * @param x       the left edge
	 * @param y       the top edge
	 * @param width   the width
	 * @param height  the height
	 * @param type    the kind of the row
	 * @param payload the payload of the row
	 */
	private record Row(int x, int y, int width, int height, RowType type, Object payload) {

		boolean contains(float mouseX, float mouseY) {
			return mouseX >= this.x && mouseX < this.x + this.width
					&& mouseY >= this.y && mouseY < this.y + this.height;
		}
	}
}
