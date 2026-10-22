package io.github.crossmod.client.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.crossmod.client.gui.render.MenuBackground;
import io.github.crossmod.client.gui.render.RenderUtils;
import io.github.crossmod.client.module.ModuleManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The CrossMod shell.
 *
 * <p>The interface is laid out in the same virtual units the game uses for its
 * own screens, so it fills the window the way the reference client does. The
 * module cards are still previews: they show the density and the styling of the
 * final grid while the module registry itself deliberately starts empty.</p>
 */
public class CrossModScreen extends GlassScreen {

	public static final String DISCORD_URL = "https://discord.gg/tDe4cZYyuV";

	private static final String TITLE = "CrossMod";
	private static final String SEARCH_HINT = "Search anything...";

	private static final int MARGIN = 4;
	private static final int SIDEBAR_WIDTH = 108;
	private static final int CONTENT_INSET = 7;
	private static final int CONTENT_RIGHT_INSET = 16;
	private static final int HEADER_HEIGHT = 47;
	private static final int GRID_GAP = 2;
	private static final int CARD_HEIGHT = 49;
	private static final int CARD_GAP = 7;
	private static final int CARD_PITCH = CARD_HEIGHT + CARD_GAP;
	private static final int COLUMN_GAP = 8;
	private static final int NAV_TOP = 53;
	private static final int NAV_STEP = 25;
	private static final int NAV_HEIGHT = 21;
	private static final int SEARCH_WIDTH = 238;
	private static final int SEARCH_HEIGHT = 29;
	private static final int CLOSE_WIDTH = 46;

	private static final String[] FILTER_NAMES = {"All", "PvP", "HUD", "Visuals", "Player"};
	private static final String[] FILTER_CATEGORIES = {null, "PvP", "HUD", "Visuals", "Player"};

	private static final int COLOR_TEXT = 0xFFECECEF;
	private static final int COLOR_TEXT_SOFT = 0xFFA7A8AD;
	private static final int COLOR_TEXT_MUTED = 0xFF7D7E84;
	private static final int COLOR_ACCENT = 0xFFD65A94;
	private static final int COLOR_ICON_OFF = 0xFF9C9DA2;
	private static final int COLOR_CLOSE = 0xFFB4B5B9;
	private static final int COLOR_CLOSE_HOVER = 0xFFF2F2F4;

	private final ModuleManager moduleManager;
	private final List<Region> regions = new ArrayList<>();
	private final List<Tile> tiles = createTiles();

	private Page page = Page.MODULES;
	private int selectedFilter;
	private float scrollOffset;
	private String searchQuery = "";
	private boolean searchFocused;
	private int blinkCounter;

	private int mouseX;
	private int mouseY;

	public CrossModScreen(ModuleManager moduleManager) {
		super(Component.literal(TITLE));
		this.moduleManager = moduleManager;
	}

	/* ------------------------------------------------------------------ */
	/* Layout                                                              */
	/* ------------------------------------------------------------------ */

	private int panelLeft() {
		return MARGIN;
	}

	private int panelTop() {
		return MARGIN;
	}

	private int panelWidth() {
		return this.width - MARGIN * 2;
	}

	private int panelHeight() {
		return this.height - MARGIN * 2;
	}

	private int panelRight() {
		return panelLeft() + panelWidth();
	}

	private int panelBottom() {
		return panelTop() + panelHeight();
	}

	private int dividerX() {
		return panelLeft() + SIDEBAR_WIDTH;
	}

	private int contentLeft() {
		return dividerX() + CONTENT_INSET;
	}

	private int contentRight() {
		return panelRight() - CONTENT_RIGHT_INSET;
	}

	private int gridTop() {
		return panelTop() + HEADER_HEIGHT + GRID_GAP;
	}

	private int gridBottom() {
		return panelBottom() - 3;
	}

	private int closeLeft() {
		return panelRight() - CLOSE_WIDTH;
	}

	private int cardWidth() {
		int available = contentRight() - contentLeft() - COLUMN_GAP;
		return Math.max(80, available / 2);
	}

	private int cardX(int column) {
		return contentLeft() + column * (cardWidth() + COLUMN_GAP);
	}

	/* ------------------------------------------------------------------ */
	/* Rendering                                                           */
	/* ------------------------------------------------------------------ */

	@Override
	public void added() {
		super.added();
		MenuBackground.capture(this.minecraft);
	}

	@Override
	public void tick() {
		this.blinkCounter++;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		this.regions.clear();

		MenuBackground.draw(graphics, this.width, this.height);
		graphics.fillGradient(0, 0, this.width, this.height, 0xDC240A15, 0xE418060E);

		drawPanel(graphics);
		drawSidebar(graphics);
		drawHeader(graphics);

		if (this.page == Page.MODULES) {
			drawModules(graphics);
		} else {
			drawPlaceholder(graphics);
		}

		// The border is painted last so it sits cleanly above the header and
		// the cards, exactly like the thin bevelled edge of the reference.
		RenderUtils.nineSlice(graphics, "panel_border.png", panelLeft(), panelTop(),
				panelWidth(), panelHeight(), 128, 128, 48, 12, 0xFFFFFFFF);
	}

	private void drawPanel(GuiGraphicsExtractor graphics) {
		RenderUtils.nineSlice(graphics, "panel_base.png", panelLeft(), panelTop(),
				panelWidth(), panelHeight(), 128, 128, 48, 12, 0xFFFFFFFF);

		int headerX = dividerX();
		RenderUtils.horizontalSlice(graphics, "header.png", headerX, panelTop(),
				panelRight() - headerX, HEADER_HEIGHT, 256, HEADER_HEIGHT * 4,
				0, 48, 0, 12, 0xFFFFFFFF);

		graphics.fill(dividerX(), panelTop(), dividerX() + 1, panelBottom(), 0x702A2B30);
		graphics.fill(dividerX() + 1, panelTop() + HEADER_HEIGHT - 1,
				panelRight(), panelTop() + HEADER_HEIGHT, 0x5527282D);
	}

	private void drawSidebar(GuiGraphicsExtractor graphics) {
		int center = panelLeft() + SIDEBAR_WIDTH / 2;
		RenderUtils.icon(graphics, "logo.png", center - 9, panelTop() + 9, 19, 0xFFFFFFFF);

		int navX = panelLeft() + 10;
		int navWidth = SIDEBAR_WIDTH - 18;

		for (int index = 0; index < Page.NAV_PAGES.size(); index++) {
			Page target = Page.NAV_PAGES.get(index);
			int y = panelTop() + NAV_TOP + index * NAV_STEP;
			boolean selected = this.page == target;
			boolean hovered = isInside(this.mouseX, this.mouseY, navX, y, navWidth, NAV_HEIGHT);

			if (selected) {
				RenderUtils.horizontalSlice(graphics, "nav_pill.png", navX, y,
						navWidth, NAV_HEIGHT, 256, NAV_HEIGHT * 4, 28, 7, 0xFFFFFFFF);
			}

			int color = selected ? COLOR_TEXT : (hovered ? 0xFFD5D5D8 : COLOR_TEXT_SOFT);
			drawText(graphics, target.label, navX + 7, y + 7, color);
			this.regions.add(new Region(navX, y, navWidth, NAV_HEIGHT, RegionType.NAV, target));
		}

		int separatorY = panelTop() + 183;
		graphics.fill(panelLeft() + 8, separatorY, dividerX() - 8, separatorY + 1, 0x382A2B30);

		int bottom = panelBottom();
		int editY = bottom - 62;
		int editX = panelLeft() + 10;
		boolean editHovered = isInside(this.mouseX, this.mouseY, editX, editY, navWidth, NAV_HEIGHT);

		RenderUtils.horizontalSlice(graphics, "nav_pill.png", editX, editY,
				navWidth, NAV_HEIGHT, 256, NAV_HEIGHT * 4, 28, 7, 0xFFFFFFFF);
		RenderUtils.icon(graphics, "edit_hud.png", editX + 7, editY + 5, 11,
				editHovered || this.page == Page.HUD ? 0xFFE8E8EA : 0xFFC2C3C6);
		drawText(graphics, "Edit HUD", editX + 23, editY + 7,
				editHovered || this.page == Page.HUD ? 0xFFF0F0F2 : 0xFFD0D0D3);
		this.regions.add(new Region(editX, editY, navWidth, NAV_HEIGHT, RegionType.EDIT_HUD, null));

		int discordY = bottom - 32;
		boolean discordHovered = isInside(this.mouseX, this.mouseY, panelLeft(), discordY - 2,
				SIDEBAR_WIDTH - 4, 22);
		int discordColor = discordHovered ? 0xFFE0E0E3 : 0xFFA8A9AE;
		RenderUtils.icon(graphics, "discord.png", panelLeft() + 15, discordY - 1, 11, discordColor);
		drawText(graphics, "Discord", panelLeft() + 34, bottom - 31, discordColor);
		this.regions.add(new Region(panelLeft(), discordY - 2, SIDEBAR_WIDTH - 4, 22,
				RegionType.DISCORD, null));
	}

	private void drawHeader(GuiGraphicsExtractor graphics) {
		if (this.page != Page.MODULES) {
			drawBoldText(graphics, this.page.label, contentLeft() + 2, panelTop() + 20, COLOR_TEXT);
			drawClose(graphics);
			return;
		}

		int searchX = contentLeft();
		int searchY = panelTop() + 6;
		RenderUtils.horizontalSlice(graphics, "search_field.png", searchX, searchY,
				SEARCH_WIDTH, SEARCH_HEIGHT, 256, 116, 32, 8, 0xFFFFFFFF);

		int iconColor = isInside(this.mouseX, this.mouseY, searchX + SEARCH_WIDTH - 24,
				searchY + 7, 16, 16) ? 0xFF9D9EA3 : 0xFF77787E;
		RenderUtils.icon(graphics, "search.png", searchX + SEARCH_WIDTH - 20,
				searchY + 9, 11, iconColor);

		int textX = searchX + 12;
		int textY = searchY + 9;
		if (this.searchQuery.isEmpty()) {
			drawText(graphics, SEARCH_HINT, textX, textY, 0xFF717278);
		} else {
			String visible = this.searchQuery;
			int maxWidth = SEARCH_WIDTH - 48;

			while (this.font.width(visible) > maxWidth && visible.length() > 1) {
				visible = visible.substring(1);
			}

			drawText(graphics, visible, textX, textY, 0xFFDADADD);

			if (this.searchFocused && this.blinkCounter / 6 % 2 == 0) {
				int cursorX = textX + this.font.width(visible) + 1;
				graphics.fill(cursorX, searchY + 8, cursorX + 1, searchY + 21, 0xFFE8E8EA);
			}
		}

		this.regions.add(new Region(searchX, searchY, SEARCH_WIDTH, SEARCH_HEIGHT,
				RegionType.SEARCH, null));

		drawFilters(graphics);
		drawClose(graphics);
	}

	private void drawFilters(GuiGraphicsExtractor graphics) {
		int right = closeLeft() - 9;
		int y = panelTop() + 10;
		int height = 21;

		for (int index = FILTER_NAMES.length - 1; index >= 0; index--) {
			String label = FILTER_NAMES[index];
			String count = Integer.toString(countFor(index));
			boolean selected = index == this.selectedFilter;
			boolean hasIcon = index == 4;
			int labelWidth = (int) Math.ceil(this.font.width(label) * 0.85F);
			int countWidth = (int) Math.ceil(this.font.width(count) * 0.62F);
			int width = 6 + (hasIcon ? 11 : 0) + labelWidth + 4 + countWidth + 6;
			int x = right - width;
			boolean hovered = isInside(this.mouseX, this.mouseY, x, y, width, height);

			RenderUtils.horizontalSlice(graphics, selected ? "filter_pill_selected.png" : "filter_pill.png",
					x, y, width, height, 256, height * 4, 40, 10, 0xFFFFFFFF);

			int labelColor = selected ? 0xFFF2F2F4 : (hovered ? 0xFFE1E1E4 : 0xFFC6C7CB);
			int countColor = selected ? 0xFFC5C5C8 : 0xFF86878D;
			int textX = x + 6;

			if (hasIcon) {
				RenderUtils.icon(graphics, "person.png", textX, y + 6, 8, labelColor);
				textX += 11;
			}

			textScaled(graphics, label, textX, y + 7, 0.85F, labelColor, false);
			textScaled(graphics, count, textX + labelWidth + 4, y + 9, 0.62F, countColor, false);

			this.regions.add(new Region(x, y, width, height, RegionType.FILTER, index));
			right = x - 5;
		}
	}

	private void drawClose(GuiGraphicsExtractor graphics) {
		int x = closeLeft();
		int width = panelRight() - x;
		boolean hovered = isInside(this.mouseX, this.mouseY, x, panelTop(), width, HEADER_HEIGHT);

		graphics.fill(x, panelTop(), x + 1, panelTop() + HEADER_HEIGHT, 0x5527282D);
		RenderUtils.icon(graphics, "close.png", x + (width - 14) / 2,
				panelTop() + 14, 14,
				hovered ? COLOR_CLOSE_HOVER : COLOR_CLOSE);
		this.regions.add(new Region(x, panelTop(), width, HEADER_HEIGHT, RegionType.CLOSE, null));
	}

	private void drawModules(GuiGraphicsExtractor graphics) {
		List<Tile> visible = filteredTiles();
		int rows = (visible.size() + 1) / 2;
		int viewportHeight = gridBottom() - gridTop();
		int contentHeight = rows * CARD_PITCH - CARD_GAP;
		int maxScroll = Math.max(0, contentHeight - viewportHeight + 3);
		this.scrollOffset = clamp(this.scrollOffset, 0.0F, (float) maxScroll);

		if (visible.isEmpty()) {
			drawEmptyState(graphics);
			return;
		}

		int scroll = Math.round(this.scrollOffset);
		int cardWidth = cardWidth();

		graphics.enableScissor(contentLeft() - 2, gridTop() - 2, contentRight() + 2, gridBottom());

		for (int index = 0; index < visible.size(); index++) {
			int column = index % 2;
			int row = index / 2;
			int x = cardX(column);
			int y = gridTop() + row * CARD_PITCH - scroll;

			if (y + CARD_HEIGHT < gridTop() - 2 || y > gridBottom() + 2) {
				continue;
			}

			drawCard(graphics, visible.get(index), x, y, cardWidth);
		}

		graphics.disableScissor();
	}

	private void drawCard(GuiGraphicsExtractor graphics, Tile tile, int x, int y, int width) {
		boolean hovered = isInside(this.mouseX, this.mouseY, x, y, width, CARD_HEIGHT);
		RenderUtils.horizontalSlice(graphics, tile.enabled ? "card_on.png" : "card_off.png",
				x, y, width, CARD_HEIGHT, 256, CARD_HEIGHT * 4, 48, 12, 0xFFFFFFFF);

		if (hovered) {
			graphics.fill(x + 14, y + 1, x + width - 14, y + 2, 0x24FFFFFF);
		}

		RenderUtils.texture(graphics, "icon_tile.png", x + 8, y + 8,
				34, 34, 136, 136, 0xFFFFFFFF);
		RenderUtils.icon(graphics, tile.icon, x + 16, y + 16, 18,
				tile.enabled ? COLOR_ACCENT : COLOR_ICON_OFF);

		drawBoldText(graphics, tile.name, x + 47, y + 14, hovered ? 0xFFF6F6F8 : COLOR_TEXT);
		textScaled(graphics, tile.description, x + 47, y + 28, 0.68F,
				hovered ? 0xFF9B9CA1 : 0xFF85868C, false);

		RenderUtils.texture(graphics, tile.enabled ? "toggle_on.png" : "toggle_off.png",
				x + width - 33, y + 18, 25, 13, 100, 52, 0xFFFFFFFF);

		this.regions.add(new Region(x, y, width, CARD_HEIGHT, RegionType.CARD, tile));
	}

	private void drawEmptyState(GuiGraphicsExtractor graphics) {
		int centerX = (contentLeft() + contentRight()) / 2;
		int centerY = (gridTop() + gridBottom()) / 2;
		RenderUtils.icon(graphics, "search.png", centerX - 12, centerY - 52, 24, 0xFF66676D);
		drawBoldText(graphics, "No modules found", centerX - this.font.width("No modules found") / 2 - 1,
				centerY - 18, 0xFFD5D5D8);
		String hint = "Try a different search or filter.";
		textScaled(graphics, hint, centerX - this.font.width(hint) * 0.8F / 2.0F,
				centerY + 2, 0.8F, 0xFF7F8086, false);
	}

	private void drawPlaceholder(GuiGraphicsExtractor graphics) {
		int centerX = (contentLeft() + contentRight()) / 2;
		int centerY = (gridTop() + gridBottom()) / 2;
		String icon = this.page == Page.HUD ? "edit_hud.png" : "external.png";

		RenderUtils.texture(graphics, "icon_tile.png", centerX - 24, centerY - 78,
				48, 48, 136, 136, 0xFFFFFFFF);
		RenderUtils.icon(graphics, icon, centerX - 12, centerY - 66, 24, COLOR_ACCENT);

		int titleWidth = this.font.width(this.page.label);
		drawBoldText(graphics, this.page.label, centerX - titleWidth / 2 - 1,
				centerY - 14, 0xFFE9E9EB);

		String hint = "This page is coming soon.";
		textScaled(graphics, hint, centerX - this.font.width(hint) * 0.82F / 2.0F,
				centerY + 4, 0.82F, 0xFF85868C, false);
	}

	/* ------------------------------------------------------------------ */
	/* Text helpers                                                        */
	/* ------------------------------------------------------------------ */

	private void drawText(GuiGraphicsExtractor graphics, String text, int x, int y, int color) {
		graphics.text(this.font, text, x, y, color, false);
	}

	private void drawBoldText(GuiGraphicsExtractor graphics, String text, int x, int y, int color) {
		graphics.text(this.font, Component.literal(text).withStyle(Style.EMPTY.withBold(true)),
				x, y, color, false);
	}

	private void textScaled(GuiGraphicsExtractor graphics, String text, float x, float y,
			float scale, int color, boolean bold) {
		var pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(x, y);
		pose.scale(scale, scale);

		if (bold) {
			graphics.text(this.font, Component.literal(text).withStyle(Style.EMPTY.withBold(true)),
					0, 0, color, false);
		} else {
			graphics.text(this.font, text, 0, 0, color, false);
		}

		pose.popMatrix();
	}

	/* ------------------------------------------------------------------ */
	/* Data                                                                */
	/* ------------------------------------------------------------------ */

	private List<Tile> filteredTiles() {
		String query = this.searchQuery.trim().toLowerCase(Locale.ROOT);
		String category = this.selectedFilter == 0 ? null : FILTER_CATEGORIES[this.selectedFilter];
		List<Tile> result = new ArrayList<>();

		for (Tile tile : this.tiles) {
			if (category != null && !category.equals(tile.category)) {
				continue;
			}

			if (query.isEmpty()
					|| tile.name.toLowerCase(Locale.ROOT).contains(query)
					|| tile.description.toLowerCase(Locale.ROOT).contains(query)
					|| tile.category.toLowerCase(Locale.ROOT).contains(query)) {
				result.add(tile);
			}
		}

		return result;
	}

	private int countFor(int filter) {
		if (filter == 0) {
			return this.tiles.size();
		}

		String category = FILTER_CATEGORIES[filter];
		int count = 0;

		for (Tile tile : this.tiles) {
			if (category.equals(tile.category)) {
				count++;
			}
		}

		return count;
	}

	private static List<Tile> createTiles() {
		List<Tile> list = new ArrayList<>();

		// The first sixteen entries reproduce the visible rows of the reference.
		list.add(new Tile("Damage Tint", "Recolor the hurt flash", "PvP", "module_damage_tint.png", true));
		list.add(new Tile("Hide Armor", "Hide armor by slot", "Player", "module_hide_armor.png", false));
		list.add(new Tile("Clean View", "Remove screen clutter", "Visuals", "module_clean_view.png", true));
		list.add(new Tile("Crosshair", "Draw your own crosshair", "HUD", "module_crosshair.png", false));
		list.add(new Tile("Crosshair Addons", "Icons around the crosshair", "HUD", "module_crosshair_addons.png", false));
		list.add(new Tile("Crossbow Tweaks", "Show loaded arrow type", "PvP", "module_crossbow.png", true));
		list.add(new Tile("Transparent Players", "See-through player models", "Visuals", "module_transparent_players.png", false));
		list.add(new Tile("Renders", "Hand shade and glowing players", "Visuals", "module_renders.png", true));
		list.add(new Tile("Pop Chams", "A ghost where a player popped a totem", "Visuals", "module_pop_chams.png", false));
		list.add(new Tile("Hand Tweaks", "Position, scale and restyle your hands", "Visuals", "module_hand.png", false));
		list.add(new Tile("Nametags", "Custom nametag display", "Player", "module_nametags.png", true));
		list.add(new Tile("TierTagger", "PvP tiers on nametags", "Player", "module_tiertagger.png", false));
		list.add(new Tile("Cobweb Tweaks", "Improve cobweb visibility", "Visuals", "module_cobweb.png", true));
		list.add(new Tile("Hitboxes", "Show entity hitboxes", "PvP", "module_hitboxes.png", true));
		list.add(new Tile("Block Overlay", "Custom block overlays", "Visuals", "module_block_overlay.png", true));
		list.add(new Tile("Keystrokes", "Display key presses", "HUD", "module_keystrokes.png", true));

		list.add(new Tile("Fullbright", "See in the dark", "Visuals", "module_visuals.png", false));
		list.add(new Tile("Armor HUD", "Show armor durability", "HUD", "module_hud.png", false));
		list.add(new Tile("Zoom", "Smooth optical zoom", "Visuals", "module_visuals.png", false));
		list.add(new Tile("Potion HUD", "Show active effects", "HUD", "module_hud.png", false));
		list.add(new Tile("No Hurt Cam", "Remove damage camera tilt", "Visuals", "module_visuals.png", false));
		list.add(new Tile("FPS Display", "Frames per second counter", "HUD", "module_hud.png", false));
		list.add(new Tile("Motion Blur", "Soft movement trails", "Visuals", "module_visuals.png", false));
		list.add(new Tile("CPS Display", "Clicks per second counter", "HUD", "module_hud.png", false));
		list.add(new Tile("Item Physics", "Physics for dropped items", "Visuals", "module_visuals.png", false));
		list.add(new Tile("Coordinates", "Show your coordinates", "HUD", "module_hud.png", false));
		list.add(new Tile("Old Animations", "Classic combat animations", "Visuals", "module_visuals.png", false));
		list.add(new Tile("Ping Display", "Show current latency", "HUD", "module_hud.png", false));
		list.add(new Tile("Particle Blocker", "Hide selected particles", "Visuals", "module_visuals.png", false));
		list.add(new Tile("Scoreboard", "Custom scoreboard display", "HUD", "module_hud.png", false));
		list.add(new Tile("View Model", "Reposition the first person view", "Visuals", "module_visuals.png", false));
		list.add(new Tile("Item Counter", "Count items in your inventory", "HUD", "module_hud.png", false));
		list.add(new Tile("Time Changer", "Change the client world time", "Visuals", "module_visuals.png", false));
		list.add(new Tile("Target HUD", "Inspect the entity you look at", "HUD", "module_hud.png", false));
		list.add(new Tile("Weather Changer", "Change the client weather", "Visuals", "module_visuals.png", false));
		list.add(new Tile("Saturation", "Show hunger saturation", "HUD", "module_hud.png", false));
		list.add(new Tile("Name Protect", "Hide your name from renders", "Player", "module_player.png", false));
		list.add(new Tile("Reach Display", "Show your reach distance", "PvP", "module_pvp.png", false));
		list.add(new Tile("Skin Layers", "Toggle outer skin layers", "Player", "module_player.png", false));
		list.add(new Tile("Combo Counter", "Count your hit combos", "PvP", "module_pvp.png", false));
		list.add(new Tile("Hit Color", "Customize hit particles", "Player", "module_player.png", false));
		list.add(new Tile("Criticals", "Always show critical particles", "PvP", "module_pvp.png", false));
		list.add(new Tile("Cape", "Show a custom cape", "Player", "module_player.png", false));
		list.add(new Tile("Auto GG", "Say GG after a game", "Misc", "module_misc.png", false));
		list.add(new Tile("Inventory Blur", "Blur the inventory background", "Misc", "module_misc.png", false));
		list.add(new Tile("Sprint", "Toggle sprint indicators", "Visuals", "module_visuals.png", false));

		return list;
	}

	/* ------------------------------------------------------------------ */
	/* Interaction                                                         */
	/* ------------------------------------------------------------------ */

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		float x = (float) event.x();
		float y = (float) event.y();

		for (int index = this.regions.size() - 1; index >= 0; index--) {
			Region region = this.regions.get(index);

			if (!region.contains(x, y)) {
				continue;
			}

			if (region.type == RegionType.SEARCH) {
				this.searchFocused = true;
				return true;
			}

			this.searchFocused = false;

			switch (region.type) {
				case FILTER -> {
					this.selectedFilter = (int) region.payload;
					this.scrollOffset = 0.0F;
				}
				case NAV -> {
					this.page = (Page) region.payload;
					this.selectedFilter = 0;
					this.scrollOffset = 0.0F;
					this.searchQuery = "";
				}
				case EDIT_HUD -> {
					this.page = Page.HUD;
					this.selectedFilter = 0;
					this.scrollOffset = 0.0F;
					this.searchQuery = "";
				}
				case CARD -> ((Tile) region.payload).enabled = !((Tile) region.payload).enabled;
				case CLOSE -> onClose();
				case DISCORD -> com.mojang.blaze3d.Blaze3D.openUri(URI.create(DISCORD_URL));
				default -> {
				}
			}

			return true;
		}

		this.searchFocused = false;
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (this.page != Page.MODULES || mouseX < contentLeft() || mouseX > contentRight()
				|| mouseY < gridTop() || mouseY > gridBottom()) {
			return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		}

		int rows = (filteredTiles().size() + 1) / 2;
		int contentHeight = rows * CARD_PITCH - CARD_GAP;
		int maxScroll = Math.max(0, contentHeight - (gridBottom() - gridTop()) + 3);
		this.scrollOffset = clamp(this.scrollOffset - (float) scrollY * 30.0F, 0.0F, (float) maxScroll);
		return true;
	}

	@Override
	protected boolean canUseToggleKey() {
		// Right Shift always closes the interface, even while the search field
		// has focus. Escape remains the way to leave the search field.
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (this.searchFocused) {
			if (event.key() == InputConstants.KEY_BACKSPACE) {
				if (!this.searchQuery.isEmpty()) {
					this.searchQuery = this.searchQuery.substring(0, this.searchQuery.length() - 1);
				}

				return true;
			}

			if (event.key() == InputConstants.KEY_ESCAPE || event.key() == InputConstants.KEY_RETURN) {
				this.searchFocused = false;
				return true;
			}
		}

		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (this.searchFocused && event.isAllowedChatCharacter()) {
			String character = event.codepointAsString();
			if (!"§".equals(character) && this.searchQuery.length() < 32) {
				this.searchQuery = this.searchQuery + character;
			}

			return true;
		}

		return super.charTyped(event);
	}

	/* ------------------------------------------------------------------ */
	/* Small helpers                                                       */
	/* ------------------------------------------------------------------ */

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}

	private static boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private record Region(int x, int y, int width, int height, RegionType type, Object payload) {
		boolean contains(float mouseX, float mouseY) {
			return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
		}
	}

	private enum RegionType {
		SEARCH,
		FILTER,
		NAV,
		CARD,
		CLOSE,
		DISCORD,
		EDIT_HUD
	}

	private enum Page {
		MODULES("Modules"),
		SETTINGS("Settings"),
		SOCIAL("Social"),
		WARDROBE("Wardrobe"),
		PROFILES("Profiles"),
		HUD("Edit HUD");

		private static final List<Page> NAV_PAGES = List.of(MODULES, SETTINGS, SOCIAL, WARDROBE, PROFILES);

		private final String label;

		Page(String label) {
			this.label = label;
		}
	}

	private static final class Tile {
		private final String name;
		private final String description;
		private final String category;
		private final String icon;
		private boolean enabled;

		private Tile(String name, String description, String category, String icon, boolean enabled) {
			this.name = name;
			this.description = description;
			this.category = category;
			this.icon = icon;
			this.enabled = enabled;
		}
	}
}
