package rsb_api.methods;

import net.runelite.api.ItemComposition;
import net.runelite.api.MenuEntry;
import net.runelite.cache.definitions.ItemDefinition;
import rsb_api.wrappers.RSItem;
import net.runelite.client.ui.FontManager;

import net.runelite.api.Point;

import java.awt.FontMetrics;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * Context menu related operations.
 */

@Slf4j
public class Menu {
    private static final Pattern HTML_TAG = Pattern
            .compile("(^[^<]+>|<[^>]+>|<[^>]+$)");

    protected static final int TOP_OF_MENU_BAR = 18;
    protected static final int MENU_ENTRY_LENGTH = 15;
    protected static final int MENU_SIDE_BORDER = 7;
    protected static final int MAX_DISPLAYABLE_ENTRIES = 32;

	private boolean LOG_MENU = false;
	private MethodContext ctx;
	Menu(final MethodContext ctx) {
		this.ctx = ctx;
	}


    /**
     * Clicks the menu target. Will left-click if the menu item is the first,
     * otherwise open menu and click the target.
     *
     * @param action The action (or action substring) to click.
     * @return <code>true</code> if the menu item was clicked; otherwise
     * <code>false</code>.
     */
    public boolean doAction(String action) {
        return doAction(action, null);
    }

    /**
     * Clicks the menu target. Will left-click if the menu item is the first,
     * otherwise open menu and click the target.
     *
     * @param action The action (or action substring) to click.
     * @param target The target (or target substring) of the action to click.
     * @return <code>true</code> if the menu item was clicked; otherwise
     * <code>false</code>.
     */
    public boolean doAction(final String action, String target) {
        int idx = getIndex(action, target);
		if (LOG_MENU) {
			log.info(String.format("action: %s, target: %s, indx: %d", action, target, idx));
		}

        if (idx == -1 || idx > MAX_DISPLAYABLE_ENTRIES) {
            while (isOpen()) {
                ctx.mouse.moveRandomly(750);
                ctx.sleepRandom(150, 250);
            }

			log.info(String.format("failed Menu.doAction() with idx %s", idx));
            return false;
        }

        if (!isOpen()) {
            if (idx == 0) {
				if (LOG_MENU) {
					log.info(String.format("left clicking action"));
				}

				log.info(String.format("Menu.doAction() - success left clicking action"));
                ctx.mouse.click(true);
                return true;
            }

			if (LOG_MENU) {
				log.info(String.format("right click - open menu"));
			}

            // ensure we don't move after
            ctx.mouse.click(false, 0);
            for (int ii=0; ii<5; ii++) {
                ctx.sleepRandom(150, 250);
                if (isOpen()) {
                    log.info(String.format("menu is now open"));
                    break;
                }
            }
        }

        if (!isOpen()) {
            log.warn(String.format("menu NOT open in doAction: %d", idx));
            return false;
        }

		// recalculate index, and then if not changed, click
        if (idx != getIndex(action, target)) {
            log.warn("menu changed underneath feet");
			return false;
		}

        return clickMain(idx);
    }

	private FontMetrics getFontMetrics() {
		return ctx.proxy.getCanvas().getGraphics().getFontMetrics(FontManager.getRunescapeBoldFont());
	}

    private boolean clickMain(final int i) {
        MenuEntry[] entries = getEntries();
        String item = (entries[i].getOption() + " " + entries[i].getTarget().replaceAll("<.*?>", ""));
        Point menu = getLocation();
        FontMetrics fm = getFontMetrics();

        int width = (fm.stringWidth(item) + MENU_SIDE_BORDER) / 2;
        int rwidth = Math.max(2, (int) (width * 0.8));
        int xOff = width + ctx.random(-rwidth, rwidth);

		if (LOG_MENU) {
			log.info(String.format("width %d, rwidth %d, xOff %d", width, rwidth, xOff));
		}

        int yOff = TOP_OF_MENU_BAR + (((MENU_ENTRY_LENGTH * i) + ctx.random(2, MENU_ENTRY_LENGTH - 2)));

        ctx.mouse.move(menu.getX() + xOff, menu.getY() + yOff);
        ctx.sleepRandom(50, 125);

        if (this.isOpen()) {
            ctx.mouse.click(true);
			log.info(String.format("Click menu success"));
            return true;
        }

        log.warn(String.format("NOT OPEN in clickMain() :("));
        return false;
    }

    /**
     * Determines if the item contains the desired action.
     *
     * @param item   The item to check.
     * @param action The item menu action to check.
     * @return <code>true</code> if the item has the action; otherwise
     * <code>false</code>.
     */
    public boolean itemHasAction(final RSItem item, final String action) {
        // Used to determine if an item is droppable/destroyable
        if (item == null) {
            return false;
        }
        ItemDefinition itemDef = item.getDefinition();
        if (itemDef != null) {
            for (String a : itemDef.getInterfaceOptions()) {
                if (a.equalsIgnoreCase(action)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Point getLocation() {
        return new Point(calculateX(), calculateY());
    }

    /**
     * Checks whether or not the menu is open.
     *
     * @return <code>true</code> if the menu is open; otherwise <code>false</code>.
     */
    public boolean isOpen() {
        return ctx.proxy.isMenuOpen();
    }

    /**
     * Strips HTML tags.
     *
     * @param input The string you want to parse.
     * @return The parsed {@code String}.
     */
    public static String stripFormatting(String input) {
        return HTML_TAG.matcher(input).replaceAll("");
    }

    /**
     * Calculates the width of the menu
     *
     * @return the menu width
     */
    protected int calculateWidth() {
        MenuEntry[] entries = getEntries();
        final int MIN_MENU_WIDTH = 102;
		FontMetrics fm = getFontMetrics();

        int longestEntry = 0;
        for (MenuEntry entry : entries) longestEntry = (fm.stringWidth(entry.getOption() + " " +
                entry.getTarget().replaceAll("<.*?>", ""))
                > longestEntry) ? fm.stringWidth(entry.getOption() + " " +
                entry.getTarget().replaceAll("<.*?>", "")) : longestEntry;
        return (longestEntry + MENU_SIDE_BORDER < MIN_MENU_WIDTH) ? MIN_MENU_WIDTH : longestEntry + MENU_SIDE_BORDER;
    }

    /**
     * Calculates the height of the menu
     *
     * @return the menu height
     */
    protected int calculateHeight() {
        MenuEntry[] entries = getEntries();
        int numberOfEntries = entries.length;
        return MENU_ENTRY_LENGTH * numberOfEntries + TOP_OF_MENU_BAR;
    }

    /**
     * Calculates the top left corner X of the menu
     *
     * @return the menu x
     */
    protected int calculateX() {
        if (isOpen()) {
            final int MIN_MENU_WIDTH = 102;
            int width = calculateWidth();
			Point p = ctx.mouse.getPressLocation();
            if (width + MENU_SIDE_BORDER < MIN_MENU_WIDTH) {
				return p.getX() - (MIN_MENU_WIDTH / 2);
			} else {
				return p.getX() - (width / 2);
			}
        }
        return -1;
    }

    /**
     * Calculates the top left corner Y of the menu
     *
     * @return the menu y
     */
    protected int calculateY() {
        if (isOpen()) {
            final int CANVAS_LENGTH = ctx.proxy.getCanvasHeight();
            MenuEntry[] entries = getEntries();

			Point p = ctx.mouse.getPressLocation();

            int offset = CANVAS_LENGTH - (p.getY() + calculateHeight());
            if (offset < 0 && entries.length >= MAX_DISPLAYABLE_ENTRIES) {
                return 0;
            }

            if (offset < 0) {
                return p.getY() + offset;
            }

            return p.getY();
        }
        return -1;
    }

    public MenuEntry[] getEntries() {
        // gets from runelite
        MenuEntry[] entries = ctx.proxy.getMenuEntries();
        MenuEntry[] reversed = new MenuEntry[entries.length];
        for (int i = entries.length - 1, x = 0; i >= 0; i--, x++)
            reversed[i] = entries[x];
        return reversed;
    }

    public String[] getEntriesString() {
        MenuEntry[] entries = getEntries();
        String[] entryStrings = new String[entries.length];
        for (int i = 0; i < entries.length; i++) {
            entryStrings[i] = stripFormatting(entries[i].getOption()) + " " + ((entries[i].getTarget() != null) ? stripFormatting(entries[i].getTarget()) : "");
        }
        return entryStrings;
    }

    /**
     * Returns the index in the menu for a given action. Starts at 0.
     *
     * @param action The action that you want the index of.
     * @return The index of the given target in the context menu; otherwise -1.
     */
    private int getIndex(String action) {
        // note this can return the first one, which might not be what you want
        action = action.toLowerCase();

        MenuEntry[] entries = getEntries();
        for (int i = 0; i < entries.length; i++) {
            if (entries[i] == null) {
                continue;
            }

            // XXX can this be null?
            String menuAction = entries[i].getOption().toLowerCase();

            if (menuAction.contains(action)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Returns the index in the menu for a given action with a given target.
     * Starts at 0.
     *
     * @param action The action of the menu entry of which you want the index.
     * @param target The target of the menu entry of which you want the index.
     *               If target is null, operates like getIndex(String action).
     * @return The index of the given target in the context menu; otherwise -1.
     */
    private int getIndex(String action, String target) {
        if (target == null) {
            return getIndex(action);
        }

        action = action.toLowerCase();
        target = target.toLowerCase();

        MenuEntry[] entries = getEntries();
        for (int i = 0; i < entries.length; i++) {
            if (entries[i] == null) {
                continue;
            }

            // XXX can these be null?
            String menuAction = entries[i].getOption().toLowerCase();
            String menuTarget = entries[i].getTarget().toLowerCase();

            if (menuAction.contains(action) && menuTarget.contains(target)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Checks whether or not a given action (or action substring) is present in
     * the menu.
     *
     * @param action The action or action substring.
     * @return <code>true</code> if present, otherwise <code>false</code>.
     */
    public boolean contains(final String action) {
        return getIndex(action) != -1;
    }

    /**
     * Checks whether or not a given action with given target is present
     * in the menu.
     *
     * @param action The action or action substring.
     * @param target The target or target substring.
     * @return <code>true</code> if present, otherwise <code>false</code>.
     */
    public boolean contains(final String action, final String target) {
        return getIndex(action, target) != -1;
    }

	public String getHoverText() {
		// used from DAX - untested
		var entries = getEntriesString();
		String item = entries[0];
		return (entries.length > 2) ? item + " / " + (entries.length - 1) + " more options" : item;
	}
}
