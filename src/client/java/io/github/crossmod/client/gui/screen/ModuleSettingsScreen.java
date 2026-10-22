package io.github.crossmod.client.gui.screen;

import io.github.crossmod.client.gui.render.GuiScale;
import io.github.crossmod.client.gui.render.RenderUtils;
import io.github.crossmod.client.gui.theme.Layout;
import io.github.crossmod.client.gui.theme.Theme;
import io.github.crossmod.client.module.Module;
import io.github.crossmod.client.module.ModuleManager;
import io.github.crossmod.client.setting.BooleanSetting;
import io.github.crossmod.client.setting.ColorSetting;
import io.github.crossmod.client.setting.ModeSetting;
import io.github.crossmod.client.setting.Setting;
import io.github.crossmod.client.setting.SliderSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The settings screen of a single module.
 *
 * <p>Every setting of the module is rendered as a row with its name on the left
 * and the control on the right. The rows are laid out on a fixed grid, so the
 * text never overlaps the controls, no matter how long the names are.</p>
 */
public final class ModuleSettingsScreen extends GlassScreen {

	private static final int ROW_HEIGHT = 30;
	private static final int CONTROL_WIDTH = 120;
	private static final int CONTROL_HEIGHT = 18;
	private static final int PANEL_WIDTH = 460;

	private final Module module;
	private final ModuleManager moduleManager;
	private final List<Row> rows = new ArrayList<>();

	private GuiScale guiScale = GuiScale.create(0);
	private float mouseX;
	private float mouseY;

	public ModuleSettingsScreen(Module module, ModuleManager moduleManager) {
		super(Component.literal(module.name()));
		this.module = module;
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
	 * Draws the panel with the header and the setting rows.
	 *
	 * @param graphics the graphics context
	 */
	private void renderPanel(GuiGraphicsExtractor graphics) {
		Theme theme = Theme.get();
		int radius = theme.cornerRadius();

		List<Setting<?>> settings = this.module.settings();

		int height = 74 + Math.max(1, settings.size()) * ROW_HEIGHT + 14;
		int x = (GuiScale.DESIGN_WIDTH - PANEL_WIDTH) / 2;
		int y = Math.max(10, (GuiScale.DESIGN_HEIGHT - height) / 2);

		RenderUtils.dropShadow(graphics, x, y, PANEL_WIDTH, height, radius, 8, 0x66);
		RenderUtils.glassPanel(graphics, x, y, PANEL_WIDTH, height, radius, 1.0F);
		RenderUtils.glassSheen(graphics, x, y, PANEL_WIDTH, height, radius);
		RenderUtils.roundedOutline(graphics, x, y, PANEL_WIDTH, height, radius, theme.outline());
		RenderUtils.glassHighlight(graphics, x, y, PANEL_WIDTH, height, radius);

		// Header
		RenderUtils.icon(graphics, "back.png", x + 14, y + 14, 16, Theme.withAlpha(theme.textSecondary(), 0xFF));
		graphics.text(this.font, this.module.name(), x + 38, y + 16, theme.textPrimary(), false);

		String description = RenderUtils.trim(this.font, this.module.description(), PANEL_WIDTH - 52);
		graphics.text(this.font, description, x + 38, y + 28, Theme.withAlpha(theme.textMuted(), 0xFF), false);

		this.rows.add(new Row(x + 8, y + 10, 24, 24, RowType.BACK, null));

		int rowY = y + 54;

		if (settings.isEmpty()) {
			graphics.text(this.font, "This module has no settings", x + 14, rowY + 4, Theme.withAlpha(theme.textMuted(), 0xFF), false);
			return;
		}

		for (Setting<?> setting : settings) {
			renderRow(graphics, setting, x + 14, rowY, PANEL_WIDTH - 28);
			rowY += ROW_HEIGHT;
		}
	}

	/**
	 * Draws a single setting row.
	 *
	 * @param graphics the graphics context
	 * @param setting  the setting
	 * @param x        the left edge
	 * @param y        the top edge
	 * @param width    the width of the row
	 */
	private void renderRow(GuiGraphicsExtractor graphics, Setting<?> setting, int x, int y, int width) {
		Theme theme = Theme.get();
		boolean hovered = isHovered(x, y, width, ROW_HEIGHT);

		if (hovered) {
			RenderUtils.roundedRect(graphics, x - 6, y - 4, width + 12, ROW_HEIGHT, 6, 0x26000000);
		}

		graphics.text(this.font, RenderUtils.trim(this.font, setting.name(), width - CONTROL_WIDTH - 24), x, y + 2,
				theme.textPrimary(), false);

		if (!setting.description().isEmpty()) {
			graphics.text(this.font, RenderUtils.trim(this.font, setting.description(), width - CONTROL_WIDTH - 24), x, y + 14,
					Theme.withAlpha(theme.textMuted(), 0xFF), false);
		}

		int controlX = x + width - CONTROL_WIDTH;
		int controlY = y + 1;

		if (setting instanceof BooleanSetting booleanSetting) {
			RenderUtils.toggleSwitch(graphics, controlX + CONTROL_WIDTH - 22, controlY + 3, 22, 12, booleanSetting.value(), theme.accent());
			this.rows.add(new Row(controlX + CONTROL_WIDTH - 22, controlY, 22, 18, RowType.TOGGLE, booleanSetting));
		} else if (setting instanceof ModeSetting modeSetting) {
			renderModeControl(graphics, modeSetting, controlX, controlY);
		} else if (setting instanceof SliderSetting sliderSetting) {
			renderSliderControl(graphics, sliderSetting, controlX, controlY);
		} else if (setting instanceof ColorSetting colorSetting) {
			renderColorControl(graphics, colorSetting, controlX, controlY);
		}
	}

	/**
	 * Draws the control of a mode setting.
	 *
	 * @param graphics the graphics context
	 * @param setting  the setting
	 * @param x        the left edge
	 * @param y        the top edge
	 */
	private void renderModeControl(GuiGraphicsExtractor graphics, ModeSetting setting, int x, int y) {
		Theme theme = Theme.get();
		boolean hovered = isHovered(x, y, CONTROL_WIDTH, CONTROL_HEIGHT);

		RenderUtils.roundedRect(graphics, x, y, CONTROL_WIDTH, CONTROL_HEIGHT, 6,
				hovered ? Theme.withAlpha(theme.accent(), 0x26) : 0x33000000);
		RenderUtils.roundedOutline(graphics, x, y, CONTROL_WIDTH, CONTROL_HEIGHT, 6,
				hovered ? Theme.withAlpha(theme.accent(), 0x99) : theme.outline());

		String value = RenderUtils.trim(this.font, setting.value(), CONTROL_WIDTH - 34);
		graphics.text(this.font, value, x + 8, y + 5, theme.textPrimary(), false);

		RenderUtils.icon(graphics, "chevron_right.png", x + CONTROL_WIDTH - 16, y + 3, 12, Theme.withAlpha(theme.textSecondary(), 0xFF));

		this.rows.add(new Row(x, y, CONTROL_WIDTH, CONTROL_HEIGHT, RowType.MODE, setting));
	}

	/**
	 * Draws the control of a slider setting.
	 *
	 * @param graphics the graphics context
	 * @param setting  the setting
	 * @param x        the left edge
	 * @param y        the top edge
	 */
	private void renderSliderControl(GuiGraphicsExtractor graphics, SliderSetting setting, int x, int y) {
		Theme theme = Theme.get();

		RenderUtils.progressBar(graphics, x, y + 7, CONTROL_WIDTH, 6, (float) setting.progress(), theme.accent());

		String value = format(setting.value());
		int valueWidth = this.font.width(value);
		graphics.text(this.font, value, x + CONTROL_WIDTH - valueWidth, y - 2, Theme.withAlpha(theme.textSecondary(), 0xFF), false);

		this.rows.add(new Row(x, y, CONTROL_WIDTH, CONTROL_HEIGHT, RowType.SLIDER, setting));
	}

	/**
	 * Draws the control of a colour setting.
	 *
	 * @param graphics the graphics context
	 * @param setting  the setting
	 * @param x        the left edge
	 * @param y        the top edge
	 */
	private void renderColorControl(GuiGraphicsExtractor graphics, ColorSetting setting, int x, int y) {
		Theme theme = Theme.get();
		boolean hovered = isHovered(x, y, CONTROL_WIDTH, CONTROL_HEIGHT);

		RenderUtils.roundedRect(graphics, x, y, CONTROL_WIDTH, CONTROL_HEIGHT, 6,
				hovered ? Theme.withAlpha(theme.accent(), 0x26) : 0x33000000);
		RenderUtils.roundedOutline(graphics, x, y, CONTROL_WIDTH, CONTROL_HEIGHT, 6,
				hovered ? Theme.withAlpha(theme.accent(), 0x99) : theme.outline());

		RenderUtils.roundedRect(graphics, x + 6, y + 4, 10, 10, 3, setting.value());
		RenderUtils.roundedOutline(graphics, x + 6, y + 4, 10, 10, 3, theme.outlineStrong());

		String value = String.format("#%06X", setting.value() & 0xFFFFFF);
		graphics.text(this.font, value, x + 22, y + 5, theme.textPrimary(), false);

		this.rows.add(new Row(x, y, CONTROL_WIDTH, CONTROL_HEIGHT, RowType.COLOR, setting));
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

			switch (row.type()) {
				case BACK -> {
					this.minecraft.setScreenAndShow(new CrossModScreen(this.moduleManager));
					return true;
				}
				case TOGGLE -> {
					((BooleanSetting) row.payload()).toggle();
					this.moduleManager.save();
					return true;
				}
				case MODE -> {
					((ModeSetting) row.payload()).cycle();
					this.moduleManager.save();
					return true;
				}
				case COLOR -> {
					ColorSetting setting = (ColorSetting) row.payload();
					setting.setValue(setting.withHue((setting.hue() + 0.08F) % 1.0F));
					this.moduleManager.save();
					return true;
				}
				case SLIDER -> {
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

		for (Row row : this.rows) {
			if (row.type() == RowType.SLIDER && row.contains(x, this.guiScale.toDesignY((float) event.y()))) {
				SliderSetting setting = (SliderSetting) row.payload();
				setting.setProgress((x - row.x()) / (float) CONTROL_WIDTH);
				this.moduleManager.save();
				return true;
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
		this.moduleManager.save();
		this.minecraft.setScreenAndShow(new CrossModScreen(this.moduleManager));
	}

	/* ------------------------------------------------------------------ */
	/* Helpers                                                             */
	/* ------------------------------------------------------------------ */

	private boolean isHovered(float x, float y, int width, int height) {
		return this.mouseX >= x && this.mouseX < x + width && this.mouseY >= y && this.mouseY < y + height;
	}

	private String format(double value) {
		if (value == Math.floor(value) && !Double.isInfinite(value)) {
			return String.valueOf((long) value);
		}

		return String.format("%.2f", value);
	}

	/**
	 * The kind of an interactive row.
	 */
	private enum RowType {
		BACK,
		TOGGLE,
		MODE,
		SLIDER,
		COLOR
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
