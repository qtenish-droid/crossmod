package io.github.crossmod.client.gui.theme;

/**
 * The metrics of the CrossMod interface.
 *
 * <p>Every value is expressed in design units, the resolution independent
 * coordinate system of the interface. The whole interface is laid out inside a
 * fixed design canvas, so it keeps its proportions and its physical size on
 * every screen and at every GUI scale.</p>
 *
 * <p>The interface is one sheet of glass: a narrow icon rail on the left and
 * three equal module columns on the right.</p>
 */
public final class Layout {

	/** Width of the complete interface, including the sidebar. */
	public static final int DESIGN_WIDTH = 600;

	/** Height reserved for the word mark above the glass panel. */
	public static final int TITLE_HEIGHT = 52;

	/** Height of the glass panel itself. */
	public static final int PANEL_HEIGHT = 296;

	/** Height of the complete interface, including the title. */
	public static final int DESIGN_HEIGHT = TITLE_HEIGHT + PANEL_HEIGHT;

	/** Left edge of the glass panel. */
	public static final int PANEL_X = 0;

	/** Top edge of the glass panel. */
	public static final int PANEL_Y = TITLE_HEIGHT;

	/** Width of the glass panel. */
	public static final int PANEL_WIDTH = DESIGN_WIDTH;

	/** Width of the icon rail on the left side of the panel. */
	public static final int SIDEBAR_WIDTH = 52;

	/** Inner padding of the module area. */
	public static final int PADDING = 14;

	/** Height of the header row that holds the search field. */
	public static final int HEADER_HEIGHT = 28;

	/** Width of the search field. */
	public static final int SEARCH_WIDTH = 200;

	/** Height of the search field. */
	public static final int SEARCH_HEIGHT = 22;

	/** Gap between two category columns. */
	public static final int COLUMN_GAP = 12;

	/** Height of a category header, including its divider. */
	public static final int COLUMN_HEADER_HEIGHT = 26;

	/** Height of a module card. */
	public static final int CARD_HEIGHT = 40;

	/** Gap between two module cards. */
	public static final int CARD_GAP = 6;

	/** Size of a sidebar button. */
	public static final int SIDEBAR_BUTTON_SIZE = 34;

	/** Gap between two sidebar buttons. */
	public static final int SIDEBAR_BUTTON_GAP = 8;

	/** Size of an icon inside a sidebar button. */
	public static final int SIDEBAR_ICON_SIZE = 16;

	private Layout() {
	}

	/**
	 * Returns the left edge of the module area.
	 *
	 * @return the left edge
	 */
	public static int contentX() {
		return SIDEBAR_WIDTH + PADDING;
	}

	/**
	 * Returns the width of the module area.
	 *
	 * @return the content width
	 */
	public static int contentWidth() {
		return DESIGN_WIDTH - SIDEBAR_WIDTH - PADDING * 2;
	}

	/**
	 * Returns the top edge of the category columns.
	 *
	 * @return the top edge
	 */
	public static int columnsY() {
		return PANEL_Y + PADDING + HEADER_HEIGHT + 8;
	}

	/**
	 * Returns the height of the category columns.
	 *
	 * @return the column height
	 */
	public static int columnsHeight() {
		return PANEL_HEIGHT - PADDING * 2 - HEADER_HEIGHT - 8;
	}

	/**
	 * Returns the width of a single category column.
	 *
	 * @return the column width
	 */
	public static int columnWidth() {
		return (contentWidth() - COLUMN_GAP * 2) / 3;
	}

	/**
	 * Returns the left edge of a category column.
	 *
	 * @param index the index of the column
	 * @return the left edge
	 */
	public static int columnX(int index) {
		return contentX() + index * (columnWidth() + COLUMN_GAP);
	}

	/**
	 * Returns the horizontal position of a sidebar button.
	 *
	 * @return the left edge
	 */
	public static int sidebarButtonX() {
		return (SIDEBAR_WIDTH - SIDEBAR_BUTTON_SIZE) / 2;
	}

	/**
	 * Returns the vertical position of a sidebar button.
	 *
	 * <p>The buttons are stacked upwards from the bottom left corner: the
	 * interface settings come first, then the Discord invite and finally the
	 * account head.</p>
	 *
	 * @param indexFromBottom the position of the button, counted from the bottom
	 * @return the top edge
	 */
	public static int sidebarButtonY(int indexFromBottom) {
		return PANEL_Y + PANEL_HEIGHT - PADDING
				- (indexFromBottom + 1) * SIDEBAR_BUTTON_SIZE
				- indexFromBottom * SIDEBAR_BUTTON_GAP;
	}
}
