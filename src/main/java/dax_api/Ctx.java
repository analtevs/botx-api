package dax_api;

import rsb_api.methods.MethodContext;
import rsb_api.wrappers.subwrap.WalkerTile;

// XXX HACK TO KEEP WORKING XXX
public class Ctx {
	public static MethodContext ctx;

	public static void init(MethodContext ctx) {
		Ctx.ctx = ctx;
	}

	// was the most ugly thing copy and pasted around the code...
	public static WalkerTile getMyLocation() {
		return new WalkerTile(new WalkerTile(Ctx.ctx.players.getMyPlayer().getLocation()));
	}
}
