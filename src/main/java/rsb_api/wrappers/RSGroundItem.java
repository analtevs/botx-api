package rsb_api.wrappers;

import net.runelite.api.Tile;
import rsb_api.methods.MethodContext;
import rsb_api.wrappers.common.Clickable07;
import rsb_api.wrappers.common.Positionable;
import rsb_api.wrappers.subwrap.WalkerTile;

/**
 * Represents an item on a tile.
 */
public class RSGroundItem implements Clickable07, Positionable {
	private final RSItem groundItem;
	private final RSTile location;


	private MethodContext ctx;
	public RSGroundItem(final MethodContext ctx, final RSTile location, final RSItem groundItem) {
		this.ctx = ctx;
		this.location = location;
		this.groundItem = groundItem;
	}

	/**
	 * Gets the top model on the tile of this ground item.
	 *
	 * @return The top model on the tile of this ground item.
	 */
	public RSModel getModel() {
		Tile tile = location.getTile(ctx);
		if (tile != null) {
			if (!tile.getGroundItems().isEmpty()) {
				for (int i = 0; i < tile.getGroundItems().size(); i++) {
					if (!tile.getGroundItems().isEmpty()) {

						return (tile.getItemLayer().getTop() != null) ?
								new RSGroundObjectModel(ctx, tile.getItemLayer().getTop().getModel(), tile) :
								new RSGroundObjectModel(ctx, tile.getGroundItems().get(i).getModel(), tile);
					}
				}
			}
		}
		return null;
	}

	/**
	 * Performs the given action on this RSGroundItem.
	 *
	 * @param action The menu action to click.
	 * @return <code>true</code> if the action was clicked; otherwise <code>false</code>.
	 */
	public boolean doAction(final String action) {
		return doAction(action, null);
	}

	/**
	 * Performs the given action on this RSGroundItem.
	 *
	 * @param action The menu action to click.
	 * @param option The option of the menu action to click.
	 * @return <code>true</code> if the action was clicked; otherwise <code>false</code>.
	 */

	public boolean doAction(final String action, final String option) {
		RSModel model = getModel();
		if (model != null) {
			return model.doAction(action, option);
		}
		return ctx.tiles.doAction(getLocation(),
								  // ZZZ more?
								  ctx.randomLinear(0.45, 0.55),
								  ctx.randomLinear(0.45, 0.55),
								  0, action, option);
	}

	public RSItem getItem() {
		return groundItem;
	}

	public WalkerTile getLocation() {
		return ctx.tiles.createWalkerTile(location);
	}

	public boolean isOnScreen() {
		RSModel model = getModel();
		if (model == null) {
			return ctx.calc.tileOnScreen(location);
		} else {
			return ctx.calc.pointOnScreen(model.getPoint());
		}
	}

	public boolean turnTo() {
		RSGroundItem item = this;
		if(item != null) {
			if(!item.isOnScreen()) {
				ctx.camera.turnTo(item.getLocation());
				return item.isOnScreen();
			}
		}
		return false;
	}

	public boolean doHover() {
		RSModel model = getModel();
		if (model == null) {
			return false;
		}
		this.getModel().hover();
		return true;
	}

	public boolean doClick() {
		RSModel model = getModel();
		if (model == null) {
			return false;
		}
		this.getModel().doClick(true);
		return true;
	}

	public boolean doClick(boolean leftClick) {
		RSModel model = getModel();
		if (model == null) {
			return false;
		}
		this.getModel().doClick(leftClick);
		return true;
	}

	public boolean isClickable() {
		RSModel model = getModel();
		if (model == null) {
			return false;
		}
		return model.getModel().isClickable();
	}
}
