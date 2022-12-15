package dax_api.walker;

import dax_api.Ctx;
import dax_api.walker.handlers.move_task.impl.DefaultObjectHandler;
import dax_api.walker.handlers.move_task.impl.DefaultWalkHandler;
import dax_api.walker.handlers.special_cases.SpecialCaseHandler;
import dax_api.walker.handlers.special_cases.SpecialCaseHandlers;
import dax_api.walker.models.MoveTask;
import dax_api.walker.models.enums.MoveActionResult;
import dax_api.walker.models.enums.Situation;
import dax_api.walker.utils.path.DaxPathFinder;
import dax_api.walker.utils.path.PathUtils;
import rsb_api.wrappers.subwrap.WalkerTile;

import java.util.ArrayList;
import java.util.List;

import static dax_api.walker.models.enums.Situation.COLLISION_BLOCKING;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DaxWalkerEngine {

    public DaxWalkerEngine() {
    }

    public boolean walkPath(List<WalkerTile> path) {
        int failAttempts = 0;

        while (failAttempts < 3) {
            MoveActionResult moveActionResult = walkNext(path);
            if (reachedEnd(path)) return true;
            if (moveActionResult == MoveActionResult.FATAL_ERROR) break;
            if (moveActionResult == MoveActionResult.SUCCESS) {
                failAttempts = 0;
            } else {
                log.info(String.format("Failed action [%d]", ++failAttempts));
            }
        }

        return false;
    }

    private boolean reachedEnd(List<WalkerTile> path) {
        if (path == null || path.size() == 0) return true;
        WalkerTile tile = Ctx.ctx.tiles.createWalkerTile(Ctx.ctx.walking.getDestination());
        return tile != null && tile.equals(path.get(path.size() - 1));
    }

    private MoveActionResult walkNext(List<WalkerTile> path) {
        MoveTask moveTask = determineNextAction(path);
        log.info("Move task: " + moveTask);

        SpecialCaseHandler specialCaseHandler = SpecialCaseHandlers.getSpecialCaseHandler(moveTask);
        if (specialCaseHandler != null) {
            log.info(String.format("Overriding normal behavior with special handler: %s",
								   specialCaseHandler.getName()));
            return specialCaseHandler.handle(moveTask);
        }

        switch (moveTask.getSituation()) {

            case COLLISION_BLOCKING:
            case DISCONNECTED_PATH:
                return new DefaultObjectHandler().handle(moveTask);

            case NORMAL_PATH_HANDLING:
                return new DefaultWalkHandler().handle(moveTask);

            case PATH_TOO_FAR:

            default:
                return MoveActionResult.FAILED;
        }
    }

    private MoveTask determineNextAction(List<WalkerTile> path) {
        WalkerTile furthestClickable = PathUtils.getFurthestReachableTileInMinimap(path);
        if (furthestClickable == null) {
            return new MoveTask(Situation.PATH_TOO_FAR, null, null);
        }

        WalkerTile next;
        try {
            next = PathUtils.getNextTileInPath(furthestClickable, path);
        } catch (PathUtils.NotInPathException e) {
            return new MoveTask(Situation.PATH_TOO_FAR, null, null);
        }

        if (next != null) {
            if (furthestClickable.distanceToDouble(next) >= 2D) {
                return new MoveTask(Situation.DISCONNECTED_PATH, furthestClickable, next);
            }

            if (!DaxPathFinder.canReach(next)) {
                return new MoveTask(COLLISION_BLOCKING, furthestClickable, next);
            }
        }

        return new MoveTask(Situation.NORMAL_PATH_HANDLING, furthestClickable, next);
    }


}
