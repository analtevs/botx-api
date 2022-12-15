package dax_api.walker.handlers.move_task.impl;

import dax_api.Ctx;
import dax_api.walker.handlers.move_task.MoveTaskHandler;
import dax_api.walker.models.MoveTask;
import dax_api.walker.models.enums.ActionResult;
import dax_api.walker.models.enums.MoveActionResult;
import dax_api.walker.utils.AccurateMouse;
import dax_api.walker.utils.TribotUtil;
import dax_api.walker.utils.camera.DaxCamera;
import dax_api.walker.utils.path.DaxPathFinder;
import net.runelite.cache.definitions.ObjectDefinition;
import rsb_api.wrappers.RSObject;
import rsb_api.wrappers.subwrap.WalkerTile;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultObjectHandler implements MoveTaskHandler {

    private static final List<String> MATCHES = Arrays.asList("Open", "Climb", "Climb-down", "Climb-up");

    @Override
    public MoveActionResult handle(MoveTask moveTask) {
        log.info("Starting...");

        if (!moveTask.getDestination().isClickable()) {
            if (!AccurateMouse.clickMinimap(moveTask.getDestination())) {
                return MoveActionResult.FAILED;
            }

            if (!waitForConditionOrNoMovement(() -> DaxPathFinder.distance(moveTask.getDestination()) < 6, 15000).isSuccess()) {
                log.info("We did not reach our destination.");
                return MoveActionResult.FAILED;
            }
        }

        RSObject closest = getClosest(moveTask);
        if (closest == null) {
            log.info("Failed to grab closest object to handle.");
            return MoveActionResult.FAILED;
        }

        log.info("We are interacting with " + TribotUtil.getName(closest));
        if (!handle(moveTask, closest, MATCHES)) {
            log.info("Failed to interact with closest object.");
            return MoveActionResult.FAILED;
        }

        ActionResult actionResult = waitForConditionOrNoMovement(() -> DaxPathFinder.canReach(moveTask.getNext()), 15000);
        log.info("Interaction result: " + actionResult);
        if (!actionResult.isSuccess()) {
            log.info("Action resulted in failed state.");
            return MoveActionResult.FAILED;
        }

        return MoveActionResult.SUCCESS;
    }

    private RSObject getClosest(MoveTask moveTask) {
        RSObject[] objects = getValid(moveTask);
        return objects.length > 0 ? objects[0] : null;
    }

    private RSObject[] getValid(MoveTask moveTask) {
        RSObject[] objects = new RSObject[]{Ctx.ctx.objects.getNearest(filter())};

        if (getDirection(moveTask) == Direction.UP) {
            log.info("This object is leading us upwards.");
            objects = Arrays.stream(objects).filter(object -> TribotUtil.getActions(object).stream().anyMatch(s -> s.toLowerCase().contains("up")))
                    .toArray(RSObject[]::new);
        }

        if (getDirection(moveTask) == Direction.DOWN) {
            log.info("This object is leading us downwards.");
            objects = Arrays.stream(objects).filter(object -> TribotUtil.getActions(object).stream().anyMatch(s -> s.toLowerCase().contains("down")))
                    .toArray(RSObject[]::new);
        }

        Arrays.sort(objects, Comparator.comparingDouble(o -> new WalkerTile(o.getLocation()).distanceToDouble(moveTask.getDestination())));
        return objects;
    }

    private Predicate<RSObject> filter() {
        return rsObject -> {
            ObjectDefinition definition = rsObject.getDef();
            return Stream.of(definition).anyMatch(rsObjectDefinition ->
                    Arrays.stream(rsObjectDefinition.getActions()).anyMatch(MATCHES::contains)
                                                 );
        };
    }

    private boolean handle(MoveTask moveTask, RSObject object, List<String> actions) {
        return handle(moveTask, object, actions.toArray(new String[0]));
    }

    private boolean handle(MoveTask moveTask, RSObject object, String... action) {
        if (!object.isOnScreen() || !object.isClickable()) {
            DaxCamera.focus(object);
        }

        String[] clickActions = action;

        if (getDirection(moveTask) == Direction.UP) {
            clickActions = TribotUtil.getActions(object).stream().filter(s -> s.toLowerCase().contains("up")).toArray(
		            String[]::new);
        }

        if (getDirection(moveTask) == Direction.DOWN) {
            clickActions = TribotUtil.getActions(object).stream().filter(s -> s.toLowerCase().contains("down")).toArray(
		            String[]::new);
        }

        log.info(String.format("Clicking %s with %s", TribotUtil.getName(object), Arrays.toString(clickActions)));
        return AccurateMouse.click(object, clickActions);
    }

    private Direction getDirection(MoveTask moveTask) {
        int playerPlane = new WalkerTile(Ctx.ctx.players.getMyPlayer().getLocation()).getWorldLocation().getPlane();
        int plane = moveTask.getNext() != null ? moveTask.getNext().getPlane() : playerPlane;
        if (plane > playerPlane) return Direction.UP;
        if (plane < playerPlane) return Direction.DOWN;
        return Direction.SAME_FLOOR;
    }

    private enum Direction {
        UP,
        DOWN,
        SAME_FLOOR
    }
}
