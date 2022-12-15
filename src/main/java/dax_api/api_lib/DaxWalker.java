package dax_api.api_lib;

import dax_api.Ctx;

import dax_api.api_lib.models.*;
//import dax_api.teleports.Teleport;
import dax_api.walker.DaxWalkerEngine;
import dax_api.walker_engine.WaitFor;
import dax_api.walker_engine.WalkerEngine;
import dax_api.walker_engine.WalkingCondition;
import dax_api.walker_engine.navigation_utils.ShipUtils;

import rsb_api.wrappers.common.Positionable;
import rsb_api.wrappers.subwrap.WalkerTile;
import rsb_api.methods.MethodContext;

import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DaxWalker {

    private static DaxWalker daxWalker;
    private static DaxWalkerEngine daxWalkerEngine;
    public static DaxWalker getInstance() {
        return daxWalker != null ? daxWalker : (daxWalker = new DaxWalker());
    }

    public boolean useRun = true;

    private WalkingCondition globalWalkingCondition;

    private DaxWalker() {
        globalWalkingCondition = () -> WalkingCondition.State.CONTINUE_WALKER;
    }

    public static WalkingCondition getGlobalWalkingCondition() {
        return getInstance().globalWalkingCondition;
    }

    public void useLocalDevelopmentServer(boolean b) {
        WebWalkerServerApi.getInstance().setTestMode(b);
    }

    public static void setGlobalWalkingCondition(WalkingCondition walkingCondition) {
        getInstance().globalWalkingCondition = walkingCondition;
    }

	private static boolean credentialSet = false;
    public static void setCredentials() {
		if (!DaxWalker.credentialSet) {
			DaxWalker.setCredentials(new DaxCredentialsProvider() {
					@Override
					public DaxCredentials getDaxCredentials() {
						return new DaxCredentials("sub_DPjXXzL5DeSiPf", "PUBLIC-KEY");
					}
				});

			log.info("DaxWalker - setCredentials");
			DaxWalker.credentialSet = true;
		}
	}

    public static void setCredentials(DaxCredentialsProvider daxCredentialsProvider) {
        WebWalkerServerApi.getInstance().setDaxCredentialsProvider(daxCredentialsProvider);
    }

    public static boolean walkTo(MethodContext ctx, Positionable positionable) {
        return walkTo(ctx, positionable, false, null);
    }

    public static boolean walkTo(MethodContext ctx,
								 Positionable positionable, boolean accurateDestination) {
        return walkTo(ctx, positionable, accurateDestination, null);
    }

    public static boolean walkTo(MethodContext ctx, Positionable destination,
								 boolean accurateDestination, WalkingCondition walkingCondition) {
		Ctx.init(ctx);
		DaxWalker.setCredentials();
        if (ShipUtils.isOnShip()) {
            ShipUtils.crossGangplank();
            WaitFor.milliseconds(500, 1200);
        }

        WalkerTile start = new WalkerTile(new WalkerTile(Ctx.ctx.players.getMyPlayer().getLocation()));

        if (start.equals(destination)) {
            return true;
        }

        List<PathRequestPair> pathRequestPairs = new ArrayList<PathRequestPair>();

        pathRequestPairs.add(new PathRequestPair(Point3D.fromPositionable(start), Point3D.fromPositionable(destination)));

	    List<PathResult> pathResults = WebWalkerServerApi.getInstance().getPaths(new BulkPathRequest(PlayerDetails.generate(),pathRequestPairs));

	    List<PathResult> validPaths = getInstance().validPaths(pathResults);

	    PathResult pathResult = getInstance().getBestPath(validPaths);
	    if (pathResult == null) {
            log.warn("No valid path found");
		    return false;
	    }

	    return WalkerEngine.getInstance().walkPath(pathResult.toWalkerTilePath(),
												   getGlobalWalkingCondition().combine(walkingCondition),
												   accurateDestination);
    }

    public List<PathResult> validPaths(List<PathResult> list) {
        List<PathResult> result =
			list.stream().filter(pathResult -> pathResult.getPathStatus() == PathStatus.SUCCESS)
			.collect(Collectors.toList());

        if (!result.isEmpty()) {
            return result;
        }

        return Collections.emptyList();
    }

    public PathResult getBestPath(List<PathResult> list) {
        return list.stream().min(Comparator.comparingInt(this::getPathMoveCost)).orElse(null);
    }

    private int getPathMoveCost(PathResult pathResult) {
		WalkerTile tile = new WalkerTile(Ctx.ctx.players.getMyPlayer().getLocation());
        if (tile.equals(pathResult.getPath().get(0).toPositionable().getLocation())) {
			return pathResult.getCost();
		}

		return pathResult.getCost();
    }
}
