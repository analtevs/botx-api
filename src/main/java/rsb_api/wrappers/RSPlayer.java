package rsb_api.wrappers;

import net.runelite.api.Actor;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.Point;
import rsb_api.methods.MethodContext;

import java.lang.ref.SoftReference;

/**
 * Represents a player.
 */
public class RSPlayer extends RSCharacter {

	private final SoftReference<Player> p;

	public RSPlayer(final MethodContext ctx, final Player p) {
		super(ctx);
		this.p = new SoftReference<>(p);
	}

	public Player getAccessor() {
		return p.get();
	}

	public Actor getInteracting() {
		Actor interacting = getAccessor().getInteracting();
		if (interacting != null) {
			return getAccessor().getInteracting();
		}
		return null;
	}

	public int getCombatLevel() {
		return p.get().getCombatLevel();
	}

	public boolean isLocalPlayerMoving() {
		if (ctx.proxy.getLocalDestinationLocation() != null) {
			return ctx.proxy.getLocalPlayer().getLocalLocation() == ctx.proxy.getLocalDestinationLocation();
		}

		return false;
	}

	@Override
	public String getName() {
		return p.get().getName();
	}

	public int getTeam() {
		return p.get().getTeam();
	}

	public boolean isIdle() {
		return getAnimation() == -1 && !isInCombat();
	}

	@Override
	public boolean doAction(final String action) {
		return doAction(action, null);
	}

	@Override
	public boolean doAction(final String action, final String target) {
		final RSModel model = getModel();
		if (model != null && isValid()) {
                    return model.doAction(action, target);
		}
		try {
			Point screenLoc;
			for (int i = 0; i < 20; i++) {
				screenLoc = getScreenLocation();
				if (!isValid() || !ctx.calc.pointOnScreen(screenLoc)) {
					return false;
				}

				if (ctx.mouse.getLocation().equals(screenLoc)) {
					break;
				}

				ctx.mouse.move(screenLoc);
			}
			MenuEntry[] entries = ctx.menu.getEntries();
			if (entries.length <= 1) {
				return false;
			}

			if (entries[0].getOption().toLowerCase().contains(action.toLowerCase())) {
				ctx.mouse.click(true);
				return true;

			} else {
				ctx.mouse.click(false);
				return ctx.menu.doAction(action, target);
			}

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public String toString() {
		return "Player[" + getName() + "]" + super.toString();
	}

	public RSTile getPosition() {
		return getLocation();
	}
}
