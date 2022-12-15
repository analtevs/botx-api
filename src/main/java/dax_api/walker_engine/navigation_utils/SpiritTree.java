package dax_api.walker_engine.navigation_utils;

import dax_api.Ctx;
import dax_api.Filters;
import dax_api.shared.helpers.InterfaceHelper;
import dax_api.walker_engine.WaitFor;
import dax_api.walker_engine.interaction_handling.InteractionHelper;
import net.runelite.rsb.util.StdRandom;
import rsb_api.wrappers.RSWidget;
import rsb_api.wrappers.subwrap.WalkerTile;

/**
 * Created by Me on 3/13/2017.
 */
public class SpiritTree {

    private static final int SPIRIT_TREE_MASTER_INTERFACE = 187;

    public enum Location {
        SPIRIT_TREE_GRAND_EXCHANGE("Grand Exchange", 3183, 3508, 0),
        SPIRIT_TREE_STRONGHOLD("Gnome Stronghold", 2461, 3444, 0),
        SPIRIT_TREE_KHAZARD("Battlefield of Khazard", 2555, 3259, 0),
        SPIRIT_TREE_VILLAGE("Tree Gnome Village", 2542, 3170, 0);

        private int x, y, z;
        private String name;
        Location(String name, int x, int y, int z){
            this.x = x;
            this.y = y;
            this.z = z;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public WalkerTile getWalkerTile(){
            return Ctx.ctx.tiles.createWalkerTile(x, y, z);
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getZ() {
            return z;
        }
    }

    public static boolean to(Location location){
        if (!Ctx.ctx.interfaces.get(SPIRIT_TREE_MASTER_INTERFACE).isValid() &&
			!InteractionHelper.click(InteractionHelper.getRSObject(Filters.Objects.actionsContains("Travel")), "Travel", () -> Ctx.ctx.interfaces.get(SPIRIT_TREE_MASTER_INTERFACE).isValid() ? WaitFor.Return.SUCCESS : WaitFor.Return.IGNORE)) {
            return false;
        }

        RSWidget option = InterfaceHelper.getAllInterfaces(SPIRIT_TREE_MASTER_INTERFACE).stream().filter(rsInterface -> {
            String text = rsInterface.getText();
            return text != null && text.contains(location.getName());
        }).findAny().orElse(null);

        if (option == null) {
            return false;
        }

        if (!option.doClick()) {
            return false;
        }

        if (WaitFor.condition(StdRandom.uniform(5400, 6500), () -> location.getWalkerTile().distanceTo(new WalkerTile(Ctx.ctx.players.getMyPlayer().getLocation())) < 10 ? WaitFor.Return.SUCCESS : WaitFor.Return.IGNORE) == WaitFor.Return.SUCCESS){
            WaitFor.milliseconds(250, 500);
            return true;
        }

        return false;
    }

}
