package dax_api.walker_engine;


import dax_api.Ctx;
import dax_api.shared.PathFindingNode;
import dax_api.walker.utils.AccurateMouse;
import dax_api.walker.utils.path.PathUtils;
import dax_api.walker_engine.bfs.BFS;
import dax_api.walker_engine.interaction_handling.PathObjectHandler;
import dax_api.walker_engine.local_pathfinding.PathAnalyzer;
import dax_api.walker_engine.local_pathfinding.Reachable;
import dax_api.walker_engine.navigation_utils.Charter;
import dax_api.walker_engine.navigation_utils.NavigationSpecialCase;
import dax_api.walker_engine.navigation_utils.ShipUtils;
import dax_api.walker_engine.real_time_collision.CollisionDataCollector;
import dax_api.walker_engine.real_time_collision.RealTimeCollisionTile;
import net.runelite.rsb.util.StdRandom;
import rsb_api.wrappers.subwrap.WalkerTile;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WalkerEngine {

    private static WalkerEngine walkerEngine;

    private int attemptsForAction;
    private final int failThreshold;
    private boolean navigating;
    private List<WalkerTile> currentPath;
	boolean accurateDestination;

    private WalkerEngine() {
        attemptsForAction = 0;
        failThreshold = 3;
        navigating = false;
        currentPath = null;
    }

    public static WalkerEngine getInstance() {
        return walkerEngine != null ? walkerEngine : (walkerEngine = new WalkerEngine());
    }

    public List<WalkerTile> getCurrentPath() {
        return currentPath;
    }

    /**
     *
     * @param path
     * @param walkingCondition
     * @return
     */
    public boolean walkPath(List<WalkerTile> path,
							WalkingCondition walkingCondition,
							boolean accurateDestination) {

        if (path.size() == 0) {
            log.info("Path is empty");
            return false;
        }

        navigating = true;
        currentPath = path;
        try {
            PathAnalyzer.DestinationDetails destinationDetails;
			attemptsForAction = 0;

            while (true) {

                if (!Ctx.ctx.game.isLoggedIn()) {
                    return false;
                }

                if (ShipUtils.isOnShip()) {
                    if (!ShipUtils.crossGangplank()) {
                        log.info("Failed to exit ship via gangplank.");
                        failedAttempt();
                    }

                    WaitFor.milliseconds(50);
                    continue;
                }

                if (isFailedOverThreshhold()) {
                    log.info("Too many failed attempts");
                    return false;
                }

                destinationDetails = PathAnalyzer.furthestReachableTile(path);
                if (PathUtils.getFurthestReachableTileInMinimap(path) == null || destinationDetails == null) {
                    log.info("Could not grab destination details.");
                    failedAttempt();
                    continue;
                }

                RealTimeCollisionTile currentNode = destinationDetails.getDestination();
                WalkerTile assumedNext = destinationDetails.getAssumed();

                //if (destinationDetails.getState() != PathAnalyzer.PathState.FURTHEST_CLICKABLE_TILE) {
				log.info(destinationDetails.toString());
				//}

                final RealTimeCollisionTile destination = currentNode;
                if (!Ctx.ctx.calc.tileOnMap(Ctx.ctx.tiles.createWalkerTile(destination.getX(), destination.getY(), destination.getZ()))) {
                    log.info("Closest tile in path is not in minimap: " + destination);
                    failedAttempt();
                    continue;
                }

                CustomConditionContainer conditionContainer = new CustomConditionContainer(walkingCondition);
                switch (destinationDetails.getState()) {
                    case DISCONNECTED_PATH:
                        if (currentNode.toWalkerTile().distanceTo(Ctx.getMyLocation()) > 10){
                            clickMinimap(currentNode, false);
                            WaitFor.milliseconds(1200, 3400);
                        }
                        NavigationSpecialCase.SpecialLocation specialLocation = NavigationSpecialCase.getLocation(currentNode.toWalkerTile()),
                                specialLocationDestination = NavigationSpecialCase.getLocation(assumedNext);
                        if (specialLocation != null && specialLocationDestination != null) {
                            log.info("[SPECIAL LOCATION] We are at " + specialLocation + " and our destination is " + specialLocationDestination);
                            if (!NavigationSpecialCase.handle(specialLocationDestination)) {
                                failedAttempt();
                            } else {
                                successfulAttempt();
                            }
                            break;
                        }

                        Charter.LocationProperty
                                locationProperty = Charter.LocationProperty.getLocation(currentNode.toWalkerTile()),
                                destinationProperty = Charter.LocationProperty.getLocation(assumedNext);
                        if (locationProperty != null && destinationProperty != null) {
                            log.info("Chartering to: " + destinationProperty);
                            if (!Charter.to(destinationProperty)) {
                                failedAttempt();
                            } else {
                                successfulAttempt();
                            }
                            break;
                        }
                        //DO NOT BREAK OUT
                    case OBJECT_BLOCKING:
                        WalkerTile walkingTile = Reachable.getBestWalkableTile(destination.toWalkerTile(), new Reachable());
                        if (isDestinationClose(destination) ||
							(walkingTile != null ? AccurateMouse.clickMinimap(walkingTile) : clickMinimap(destination, false))) {
                            log.info("Handling Object...");
                            if (PathObjectHandler.handle(destinationDetails, path)) {
                                successfulAttempt();
                            } else {
                                failedAttempt();
                            }
                        }

                        break;

                    case FURTHEST_CLICKABLE_TILE:
                        if (clickMinimap(currentNode, false)) {
                            long offsetWalkingTimeout = System.currentTimeMillis() + StdRandom.uniform(2500, 4000);

                            WaitFor.condition(10000, () -> {
                                switch (conditionContainer.trigger()) {
                                    case EXIT_OUT_WALKER_SUCCESS:
                                    case EXIT_OUT_WALKER_FAIL:
                                        return WaitFor.Return.SUCCESS;
                                }

                                PathAnalyzer.DestinationDetails furthestReachable = PathAnalyzer.furthestReachableTile(path);
                                PathFindingNode currentDestination = BFS.bfsClosestToPath(path,
																						  RealTimeCollisionTile.get(destination.getX(),
																													destination.getY(),
																													destination.getZ()));
                                if (currentDestination == null) {
                                    log.info("Could not walk to closest tile in path.");
                                    failedAttempt();
                                    return WaitFor.Return.FAIL;
                                }

                                int indexCurrentDestination = path.indexOf(currentDestination.toWalkerTile());

                                PathFindingNode closestToPlayer = PathAnalyzer.closestTileInPathToPlayer(path);
                                if (closestToPlayer == null) {
                                    log.info("Could not detect closest tile to player in path.");
                                    failedAttempt();
                                    return WaitFor.Return.FAIL;
                                }

                                int indexCurrentPosition = path.indexOf(closestToPlayer.toWalkerTile());
                                if (furthestReachable == null) {
                                    log.warn("Furthest reachable is null");
                                    return WaitFor.Return.FAIL;
                                }

                                int indexNextDestination = path.indexOf(furthestReachable.getDestination().toWalkerTile());
                                if (indexNextDestination - indexCurrentDestination > 5 ||
									indexCurrentDestination - indexCurrentPosition < 5) {
                                    return WaitFor.Return.SUCCESS;
                                }

                                if (System.currentTimeMillis() > offsetWalkingTimeout &&
									!Ctx.ctx.players.getMyPlayer().isLocalPlayerMoving()) {
									log.info("offsetWalkingTimeout");
                                    return WaitFor.Return.FAIL;
                                }

                                return WaitFor.milliseconds(400, 800);
                            });
                        }

                        break;

                    case END_OF_PATH:
						if (accurateDestination) {
							clickMinimap(destinationDetails.getDestination(), true);
						}

                        log.info("Reached end of path");
                        return true;
                }

                switch (conditionContainer.getResult()) {
                    case EXIT_OUT_WALKER_SUCCESS:
                        return true;
                    case EXIT_OUT_WALKER_FAIL:
                        return false;
                }

                WaitFor.milliseconds(400, 800);

            }
        } finally {
            navigating = false;
        }
    }

    boolean isNavigating() {
        return navigating;
    }

    boolean isDestinationClose(PathFindingNode pathFindingNode){
        final WalkerTile playerPosition = Ctx.getMyLocation();

		var tile = pathFindingNode.toWalkerTile();
        return (tile.isClickable() &&
				playerPosition.distanceTo(tile) <= 16 &&
				BFS.isReachable(RealTimeCollisionTile.get(playerPosition.getX(),
														  playerPosition.getY(),
														  playerPosition.getPlane()),
								RealTimeCollisionTile.get(pathFindingNode.getX(),
														  pathFindingNode.getY(),
														  pathFindingNode.getZ()), 200));
    }

    public boolean clickMinimap(PathFindingNode pathFindingNode, boolean lastClick){
        final WalkerTile playerPosition = Ctx.getMyLocation();

        if (playerPosition.distanceTo(pathFindingNode.toWalkerTile()) <= 1){
            return true;
        }

		PathFindingNode randomNearby = BFS.getRandomTileNearby(pathFindingNode);
        if (randomNearby == null) {
            log.info("Unable to generate randomization.");
            return false;
        }


		log.info("XRandomize(" + pathFindingNode.getX() + "," + pathFindingNode.getY() +
				 "," + pathFindingNode.getZ() + ") -> (" + randomNearby.getX() + "," +
				 randomNearby.getY() + "," + randomNearby.getZ() + ")");

		boolean result = AccurateMouse.clickMinimap(Ctx.ctx.tiles.createWalkerTile(randomNearby.getX(),
																				   randomNearby.getY(),
																				   randomNearby.getZ()));

        return result || AccurateMouse.clickMinimap(Ctx.ctx.tiles.createWalkerTile(pathFindingNode.getX(),
																				   pathFindingNode.getY(),
																				   pathFindingNode.getZ()));
    }

    public void hoverMinimap(PathFindingNode pathFindingNode){
        if (pathFindingNode == null) {
            return;
        }

        Ctx.ctx.mouse.move(Ctx.ctx.calc.tileToMinimap(Ctx.ctx.tiles.createWalkerTile(pathFindingNode.getX(), pathFindingNode.getY(), pathFindingNode.getZ())));
    }

    private boolean successfulAttempt() {
        attemptsForAction = 0;
        return true;
    }

    private void failedAttempt() {
        if (Ctx.ctx.camera.getPitch() < 90) {
            Ctx.ctx.camera.setPitch(StdRandom.uniform(90, 100));
        }

        if (++attemptsForAction > 1) {
            Ctx.ctx.camera.setAngle(StdRandom.uniform(0, 360));
        }

        log.info("Failed attempt on action.");

        WaitFor.milliseconds(450 * (attemptsForAction + 1), 850 * (attemptsForAction + 1));
        CollisionDataCollector.generateRealTimeCollision();
    }

    private boolean isFailedOverThreshhold() {
        return attemptsForAction >= failThreshold;
    }

    private class CustomConditionContainer {
        private WalkingCondition walkingCondition;
        private WalkingCondition.State result;
        CustomConditionContainer(WalkingCondition walkingCondition){
            this.walkingCondition = walkingCondition;
            this.result = WalkingCondition.State.CONTINUE_WALKER;
        }
        public WalkingCondition.State trigger(){
            result = (walkingCondition != null ? walkingCondition.action() : result);
            return result != null ? result : WalkingCondition.State.CONTINUE_WALKER;
        }
        public WalkingCondition.State getResult() {
            return result;
        }
    }
}
