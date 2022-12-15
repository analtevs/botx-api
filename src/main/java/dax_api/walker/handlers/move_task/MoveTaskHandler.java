package dax_api.walker.handlers.move_task;

import dax_api.walker.models.MoveTask;
import dax_api.walker.models.WaitCondition;
import dax_api.walker.models.enums.ActionResult;
import dax_api.walker.models.enums.MoveActionResult;
import dax_api.walker_engine.WaitFor;
import net.runelite.rsb.util.Timer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public interface MoveTaskHandler {

    MoveActionResult handle(MoveTask moveTask);

    /**
     * @param waitCondition
     * @param timeout
     * @return If player stops moving, return fail condition.
     */
    default ActionResult waitForConditionOrNoMovement(WaitCondition waitCondition, long timeout) {
        long startTime = System.currentTimeMillis();
        AtomicLong lastMoved = new AtomicLong(System.currentTimeMillis());
        return waitFor(waitCondition, timeout);
    }

    default ActionResult waitFor(WaitCondition waitCondition, long timeout) {
        long end = System.currentTimeMillis() + timeout;
        while (System.currentTimeMillis() < end) {
            if (waitCondition.action()) {
				return ActionResult.CONTINUE;
			}

            WaitFor.milliseconds(80, 150);
        }
        return ActionResult.FAILURE;
    }

}
