package dax_api.walker_engine.navigation_utils;

import dax_api.Ctx;
import dax_api.Filters;
import dax_api.walker_engine.WaitFor;
import dax_api.walker_engine.interaction_handling.InteractionHelper;
import net.runelite.rsb.util.StdRandom;
import rsb_api.wrappers.RSArea;
import rsb_api.wrappers.RSObject;
import rsb_api.wrappers.RSTile;
import rsb_api.wrappers.subwrap.WalkerTile;



public class ShipUtils {

    private static final RSTile[] SPECIAL_CASES = new RSTile[]{new RSTile(2663, 2676, 1)};

    public static boolean isOnShip() {
        WalkerTile playerPos = Ctx.getMyLocation();
        for (RSTile specialCase : SPECIAL_CASES){
            if (new RSArea(Ctx.ctx.tiles.createWalkerTile(specialCase), 5).contains(playerPos)) {
                return true;
            }
        }
        return getGangplank() != null
                && new WalkerTile(Ctx.ctx.players.getMyPlayer().getLocation()).getWorldLocation().getPlane() == 1
                && Ctx.ctx.objects.getAll(Filters.Objects.nameEquals("Ship's wheel", "Ship's ladder", "Anchor")).length > 0;
    }

    public static boolean crossGangplank() {
        RSObject gangplank = getGangplank();
        if (gangplank == null){
            return false;
        }
        if (!gangplank.doAction("Cross")){
            return false;
        }
        //if (WaitFor.condition(1000, () -> Ctx.ctx.game.getCrosshairState() == 2 ? WaitFor.Return.SUCCESS : WaitFor.Return.IGNORE) != WaitFor.Return.SUCCESS){
        //    return false;
        //}

        return WaitFor.condition(StdRandom.uniform(2500, 3000), () -> !ShipUtils.isOnShip() ? WaitFor.Return.SUCCESS : WaitFor.Return.IGNORE) == WaitFor.Return.SUCCESS;
    }

    private static RSObject getGangplank(){
        return InteractionHelper.getRSObject(Filters.Objects.nameEquals("Gangplank").combine(Filters.Objects.actionsContains("Cross"), true));
    }

}
