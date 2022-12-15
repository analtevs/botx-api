package dax_api.walker_engine.interaction_handling;

import dax_api.Ctx;

import dax_api.walker.utils.AccurateMouse;
import dax_api.walker_engine.WaitFor;
import net.runelite.rsb.utils.Filter;
import net.runelite.rsb.util.StdRandom;
import net.runelite.rsb.util.Timer;
import rsb_api.wrappers.*;
import rsb_api.wrappers.common.Clickable07;
import rsb_api.wrappers.common.Positionable;
import rsb_api.wrappers.subwrap.WalkerTile;


public class InteractionHelper {

    public static boolean click(Clickable07 clickable, String... actions){
        return click(clickable, actions, null);
    }

    public static boolean click(Clickable07 clickable, String action, WaitFor.Condition condition){
        return click(clickable, new String[]{action}, condition);
    }

    /**
     * Interacts with nearby object and waits for {@code condition}.
     *
     * @param clickable clickable entity
     * @param actions actions to click
     * @param condition condition to wait for after the click action
     * @return if {@code condition} is null, then return the outcome of condition.
     *          Otherwise, return the result of the click action.
     */
    public static boolean click(Clickable07 clickable, String[] actions, WaitFor.Condition condition){
        if (clickable == null){
            return false;
        }

        if (clickable instanceof RSItem){
            boolean b = false;
            for (String action : actions) {
                b = clickable.doAction(action) && (condition == null || WaitFor.condition(StdRandom.uniform(7000, 8000), condition) == WaitFor.Return.SUCCESS);
                if (b) {
                    break;
                }
            }
            return b;
        }

        WalkerTile position = ((Positionable) clickable).getLocation();

        if (!isOnScreenAndClickable(clickable)){
            Ctx.ctx.walking.walkTo(Ctx.ctx.walking.randomizeTile(position, 10, 10));
        }

        WaitFor.Return result = WaitFor.condition(WaitFor.getMovementRandomSleep(position), new WaitFor.Condition() {
            final long startTime = System.currentTimeMillis();
            @Override
            public WaitFor.Return active() {
                if (isOnScreenAndClickable(clickable)){
                    return WaitFor.Return.SUCCESS;
                }
                if (Timer.timeFromMark(startTime) > 2000 && !Ctx.ctx.players.getMyPlayer().isLocalPlayerMoving()){
                    return WaitFor.Return.FAIL;
                }
                return WaitFor.Return.IGNORE;
            }
        });

        if (result != WaitFor.Return.SUCCESS){
            return false;
        }

        if (!AccurateMouse.click(clickable, actions)){
            if (Ctx.ctx.camera.getAngle() < 90){
                Ctx.ctx.camera.setPitch(StdRandom.uniform(90, 100));
            }
            return false;
        }

        return condition == null || WaitFor.condition(StdRandom.uniform(7000, 8500), condition) == WaitFor.Return.SUCCESS;
    }

    public static RSItem getRSItem(Filter<RSItem> filter){
        RSItem[] rsItems = Ctx.ctx.inventory.find(filter);
        return rsItems.length > 0 ? rsItems[0] : null;
    }

    public static RSNPC getRSNPC(Filter<RSNPC> filter){
        return Ctx.ctx.npcs.getNearest(filter);
    }

    public static RSObject getRSObject(Filter<RSObject> filter){
        return Ctx.ctx.objects.getNearest(15, filter);
    }

    public static RSGroundItem getRSGroundItem(Filter<RSGroundItem> filter){
        return Ctx.ctx.groundItems.getNearest(filter);
    }

    public static boolean focusCamera(Clickable07 clickable){
        if (clickable == null){
            return false;
        }
        if (isOnScreenAndClickable(clickable)){
            return true;
        }
        WalkerTile tile = ((Positionable) clickable).getLocation();
        Ctx.ctx.camera.turnTo(tile);
        Ctx.ctx.camera.setPitch(100 - (tile.distanceTo(Ctx.getMyLocation()) * 4));
        return isOnScreenAndClickable(clickable);
    }

    private static boolean isOnScreenAndClickable(Clickable07 clickable){
        if (clickable instanceof RSCharacter && !((RSCharacter) clickable).isOnScreen()){
            return false;
        }
        if (clickable instanceof RSObject && !((RSObject) clickable).isOnScreen()){
            return false;
        }
        if (clickable instanceof RSGroundItem && !((RSGroundItem) clickable).isOnScreen()){
            return false;
        }
        return clickable.isClickable();
    }


}
