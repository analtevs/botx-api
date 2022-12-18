package rsb_api.methods;

import net.runelite.api.MenuEntry;
import net.runelite.client.ui.FontManager;

import net.runelite.api.Point;

import lombok.extern.slf4j.Slf4j;

import java.awt.FontMetrics;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.Collections;


/**
 * Context menu related operations.
 * XXX Doesn't support stretched or anything like that?
 */

@Slf4j
public class Menu {
    private final Pattern HTML_TAG = Pattern.compile("(^[^<]+>|<[^>]+>|<[^>]+$)");
    private final int TOP_OF_MENU_BAR = 18;

	// XXX isnt the height not length?
    private final int MENU_ENTRY_LENGTH = 15;

    private final int MENU_SIDE_BORDER = 7;
    private final int MAX_DISPLAYABLE_ENTRIES = 32;

	private boolean LOG_MENU_DEBUG = true;

	private MethodContext ctx;

	Menu(final MethodContext ctx) {
		this.ctx = ctx;
	}


    /**
     * Strips HTML tags.
     *
     * @param input The string you want to parse.
     * @return The parsed {@code String}.
     */
    private String stripFormatting(String input) {
        return HTML_TAG.matcher(input).replaceAll("");
    }

	private FontMetrics getFontMetrics() {
		return ctx.proxy.getCanvas().getGraphics().getFontMetrics(FontManager.getRunescapeBoldFont());
	}

    private boolean clickMain(final int i) {
		int x = ctx.proxy.getMenuX();
		int y = ctx.proxy.getMenuY();
        int w = ctx.proxy.getMenuWidth();
        int h = ctx.proxy.getMenuHeight();

        int mid = w / 2;
        int rwidth = Math.max(4, w / 3);
        int xOff = mid + ctx.random(-rwidth, rwidth);
        int yOff = TOP_OF_MENU_BAR + (((MENU_ENTRY_LENGTH * i) + ctx.random(2, MENU_ENTRY_LENGTH - 2)));


		if (LOG_MENU_DEBUG) {
			log.info("xx {}, yy {} ww {} hh {}", x, y, w, h);
			log.info("mid {}, xOff {}", mid, xOff);
		}

		x += xOff;
		y += yOff;

        ctx.mouse.move(x, y);
		// XXX ZZZ this seems insanely fast - needs to be configurable
		ctx.sleepRandom(50, 125);

        if (!this.isOpen()) {
			log.warn("NOT OPEN anymore in clickMain() :(");
			return false;
		}

		if (!ctx.mouse.isPresent()) {
			log.warn("Mouse moved offscreen");
			// XXX dump everything here
			return false;
		}

		ctx.mouse.click(true);
		if (LOG_MENU_DEBUG) {
			log.info("Click menu success");
		}

		return true;
    }

    /**
     * Calculates the width of the menu
     *
     * @return the menu width
     */
    // private int calculateWidth() {
    //     MenuEntry[] entries = getEntries();
    //     final int MIN_MENU_WIDTH = 102;
	// 	FontMetrics fm = getFontMetrics();

    //     int longestEntry = 0;
    //     for (MenuEntry entry : entries) {
	// 		var l = fm.stringWidth(entry.getOption() + " " + entry.getTarget().replaceAll("<.*?>", ""));
	// 		if (l > longestEntry) {
	// 			longestEntry = l;
	// 		}
	// 	}

    //     return Math.max(longestEntry + MENU_SIDE_BORDER, MIN_MENU_WIDTH);
    // }

    // /**
    //  * Calculates the height of the menu
    //  *
    //  * @return the menu height
    //  */
    // private int calculateHeight() {
    //     MenuEntry[] entries = getEntries();
    //     int numberOfEntries = entries.length;
    //     return MENU_ENTRY_LENGTH * numberOfEntries + TOP_OF_MENU_BAR;
    // }

    // /**
    //  * Calculates the top left corner X of the menu
    //  *
    //  * @return the menu x
    //  */
    // private int calculateX() {
    //     if (isOpen()) {
    //         final int MIN_MENU_WIDTH = 102;
    //         int width = calculateWidth();
	// 		Point p = ctx.mouse.getPressLocation();
    //         if (width + MENU_SIDE_BORDER < MIN_MENU_WIDTH) {
	// 			return p.getX() - (MIN_MENU_WIDTH / 2);
	// 		} else {
	// 			return p.getX() - (width / 2);
	// 		}
    //     }

    //     return -1;
    // }

    // /**
    //  * Calculates the top left corner Y of the menu
    //  *
    //  * @return the menu y
    //  */
    // private int calculateY() {
    //     if (isOpen()) {
    //         final int CANVAS_LENGTH = ctx.proxy.getCanvasHeight();
    //         MenuEntry[] entries = getEntries();

	// 		Point p = ctx.mouse.getPressLocation();

    //         int offset = CANVAS_LENGTH - (p.getY() + calculateHeight());
    //         if (offset < 0 && entries.length >= MAX_DISPLAYABLE_ENTRIES) {
    //             return 0;
    //         }

    //         if (offset < 0) {
    //             return p.getY() + offset;
    //         }

    //         return p.getY();
    //     }
    //     return -1;
    // }

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

            String menuAction = entries[i].getOption().toLowerCase();
            String menuTarget = entries[i].getTarget().toLowerCase();

            if (menuAction.contains(action) && menuTarget.contains(target)) {
                return i;
            }
        }

        return -1;
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
		if (LOG_MENU_DEBUG) {
			log.info("action: {}, target: {}, indx: {}", action, target, idx);
		}

        if (idx == -1 || idx > MAX_DISPLAYABLE_ENTRIES) {
            while (isOpen()) {
                ctx.mouse.moveRandomly(750);
                ctx.sleepRandom(150, 250);
            }

			log.info("failed Menu.doAction() with idx {}", idx);
            return false;
        }

        if (!isOpen()) {
            if (idx == 0) {
				if (LOG_MENU_DEBUG) {
					log.info("left clicking action");
					log.info("Menu.doAction() - success left clicking action");
				}

                ctx.mouse.click(true);
                return true;
            }

			if (LOG_MENU_DEBUG) {
				log.info("right click - open menu");
			}

            // ensure we don't move after
            ctx.mouse.click(false, 0);
            for (int ii=0; ii<5; ii++) {
                ctx.sleepRandom(150, 250);
                if (isOpen()) {
					if (LOG_MENU_DEBUG) {
						log.info("menu is now open");
					}
                    break;
                }
            }
        }

        if (!isOpen()) {
            log.warn("menu NOT open in doAction: {}", idx);
            return false;
        }

		// recalculate index, and then if not changed, click
        if (idx != getIndex(action, target)) {
            log.warn("menu changed underneath feet");
			return false;
		}

        return clickMain(idx);
    }

    public MenuEntry[] getEntries() {
        // gets from runelite
        MenuEntry[] entries = ctx.proxy.getMenuEntries();

		// XXX surely can just let java do this?
		Collections.reverse(Arrays.asList(entries));
		return entries;

        // MenuEntry[] reversed = new MenuEntry[entries.length];
        // for (int i = entries.length - 1, x = 0; i >= 0; i--, x++) {
        //     reversed[i] = entries[x];
		// }
        // return reversed;
    }

    public String[] getEntriesString() {
        MenuEntry[] entries = getEntries();
        String[] entryStrings = new String[entries.length];
        for (int i = 0; i < entries.length; i++) {
            entryStrings[i] = stripFormatting(entries[i].getOption()) + " " + ((entries[i].getTarget() != null) ? stripFormatting(entries[i].getTarget()) : "");
        }
        return entryStrings;
    }

	public String getHoverText() {
		// used from DAX - untested
		var entries = getEntriesString();
		String item = entries[0];
		return (entries.length > 2) ? item + " / " + (entries.length - 1) + " more options" : item;
	}

    /**
     * Checks whether or not the menu is open.
     *
     * @return <code>true</code> if the menu is open; otherwise <code>false</code>.
     */
    public boolean isOpen() {
        return ctx.proxy.isMenuOpen();
    }

	public void enableDebug(boolean value) {
		LOG_MENU_DEBUG = value;
	}
}
