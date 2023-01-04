package rsb_api.methods;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Point;
import net.runelite.rsb.internal.input.InputManager;

@Slf4j
public class Mouse {
    /**
     * The maximum distance (in pixels) to move the mouse after clicks in either
     * direction on both axes.
     */
    private int defaultMaxMoveAfter = 10;
    private int tempDefaultMaxMoveAfter = 0;

    private final MethodContext ctx;
    private final InputManager inputManager;

    Mouse(MethodContext ctx, InputManager im) {
        this.ctx = ctx;
        inputManager = im;
    }

    public void pushDefaultMoveAfter(int moveAfter) {
        tempDefaultMaxMoveAfter = defaultMaxMoveAfter;
        defaultMaxMoveAfter = moveAfter;
    }

    public void popDefaultMoveAfter() {
        defaultMaxMoveAfter = tempDefaultMaxMoveAfter;
    }

    /**
     * Author - Enfilade Moves the mouse a random distance between 1 and
     * maxDistance from the current position of the mouse by generating a random
     * vector and then multiplying it by a random number between 1 and
     * maxDistance. The maximum distance is cut short if the mouse would go off
     * screen in the direction it chose.
     *
     * @param maxDistance The maximum distance the cursor will move (exclusive)
     */
    public void moveRandomly(final int maxDistance) {
        moveRandomly(1, maxDistance);
    }

    /**
     * Author - Enfilade Moves the mouse a random distance between minDistance
     * and maxDistance from the current position of the mouse by generating
     * random vector and then multiplying it by a random number between
     * minDistance and maxDistance. The maximum distance is cut short if the
     * mouse would go off screen in the direction it chose.
     *
     * @param minDistance The minimum distance the cursor will move
     * @param maxDistance The maximum distance the cursor will move (exclusive)
     */
    public void moveRandomly(final int minDistance, final int maxDistance) {
        /* Generate a random vector for the direction the mouse will move in */
        double xvec = Math.random();
        if (ctx.random(0, 2) == 1) {
            xvec = -xvec;
        }
        double yvec = Math.sqrt(1 - xvec * xvec);
        if (ctx.random(0, 2) == 1) {
            yvec = -yvec;
        }
        /* Start the maximum distance at maxDistance */
        double distance = maxDistance;

        /* Get the current location of the cursor */
        Point p = getLocation();

        /* Calculate the x coordinate if the mouse moved the maximum distance */
        int maxX = (int) Math.round(xvec * distance + p.getX());
        /*
         * If the maximum x is offscreen, subtract that distance/xvec from the
         * maximum distance so the maximum distance will give a valid X
         * coordinate
         */
        distance -= Math.abs((maxX - Math.max(0, Math.min(ctx.game.getWidth(), maxX))) / xvec);

        /* Do the same thing with the Y coordinate */
        int maxY = (int) Math.round(yvec * distance + p.getY());
        distance -= Math.abs((maxY - Math.max(0, Math.min(ctx.game.getHeight(), maxY))) / yvec);
        /*
         * If the maximum distance in the generated direction is too small,
         * don't move the mouse at all
         */
        if (distance < minDistance) {
            return;
        }
        /*
         * With the calculated maximum distance, pick a random distance to move
         * the mouse between maxDistance and the calculated maximum distance
         */
        distance = ctx.random(minDistance, (int) distance);
        /* Generate the point to move the mouse to and move it there */
        move((int) (xvec * distance) + p.getX(), (int) (yvec * distance) + p.getY());
    }

    /**
     * Moves the mouse off the screen in a random direction.
     */
    public void moveOffScreen() {
        if (isPresent()) {
			switch (ctx.random(0, 10)) {
				case 0 -> // up
						move(ctx.random(-10, ctx.game.getWidth() + 10), ctx.random(-100, -10));
				case 1 -> // down
						move(ctx.random(-10, ctx.game.getWidth() + 10), ctx.game.getHeight() + ctx.random(10, 100));
				// left
				// left
				// left
				case 2, 3, 4, 5 -> // left
						move(ctx.random(-100, -10), ctx.random(-10, ctx.game.getHeight() + 10));
				// right
				// right
				// right
				case 6, 7, 8, 9 -> // right
						move(ctx.random(10, 100) + ctx.game.getWidth(), ctx.random(-10, ctx.game.getHeight() + 10));
			}
        }
    }

    public void click(final boolean leftClick) {
        click(leftClick, defaultMaxMoveAfter);
    }

    public void click(final int x, final int y, final boolean leftClick) {
        click(x, y, 0, 0, leftClick);
    }

    public void click(final int x, final int y, final int randX, final int randY, final boolean leftClick) {
        click(x, y, randX, randY, leftClick, this.defaultMaxMoveAfter);
    }

    /**
     * Moves the mouse to a given location with given randomness then clicks,
     * then moves a random distance up to <code>afterOffset</code>.
     *
     * @param x             x coordinate
     * @param y             y coordinate
     * @param randX         x randomness (added to x)
     * @param randY         y randomness (added to y)
     * @param leftClick     <code>true</code> to left-click, <code>false</code>to right-click.
     * @param moveAfterDist The maximum distance in pixels to move on both axes shortly
     *                      after moving to the destination.
     */
    public synchronized void click(final int x, final int y, final int randX, final int randY, final boolean leftClick, final int moveAfterDist) {
        move(x, y, randX, randY);
        ctx.sleepRandom(100, 200);
        click(leftClick, moveAfterDist);
    }

    public synchronized void click(final boolean leftClick, final int moveAfterDist) {
        inputManager.clickMouse(leftClick);
        if (moveAfterDist > 0) {
            // ZZZ slower?
            ctx.sleepRandom(150, 350);
            Point pos = getLocation();
            move(pos.getX() - moveAfterDist, pos.getY() - moveAfterDist, moveAfterDist * 2, moveAfterDist * 2);
        }
    }

    public void move(final int x, final int y) {
        move(x, y, 0, 0);
    }

    public void move(final Point p) {
        move(p.getX(), p.getY(), 0, 0);
    }

    /**
     * Moves the mouse to the specified point then adds random distance within to randX and randY
     *
     * @param p     The x and y destination.
     * @param randX X-axis randomness (added to x).
     * @param randY X-axis randomness (added to y).
     */
    public void move(final Point p, final int randX, final int randY) {
        move(p.getX(), p.getY(), randX, randY);
    }

    public void move(int x, int y, final int randX, final int randY) {
        if (randX > 0) {
            double sd = Math.max(2.0, randX / 2.0);
            x += ctx.random(-randX, randX, sd);
        }

        if (randY > 0) {
            double sd = Math.max(2.0, randY / 2.0);
            y += ctx.random(-randY, randY, sd);
        }

        inputManager.windMouse(x, y);
    }

    /**
     * Hops mouse to the specified coordinate.
     *
     * @param x The x coordinate.
     * @param y The y coordinate
     */
    public synchronized void hop(final int x, final int y) {
        inputManager.hopMouse(x, y);
    }

    /**
     * Hops mouse to the specified point.
     *
     * @param p The coordinate point.
     * @see #hop(Point)
     */
    public void hop(final Point p) {
        hop(p.getX(), p.getY());
    }

    /**
     * Moves the mouse slightly depending on where it currently is.
     */
    public void moveSlightly() {
        Point p = new Point((int) (getLocation().getX() + (Math.random() * 50 > 25 ? 1 : -1) * (30 + Math.random() * 90)), (int) (getLocation().getY() + (Math.random() * 50 > 25 ? 1 : -1) * (30 + Math.random() * 90)));
        if (p.getX() < 1 || p.getY() < 1 || p.getX() > 761 || p.getY() > 499) {
            moveSlightly();
            return;
        }
        move(p);
    }

    public Point getLocation() {
        var vm = inputManager.getVirtualMouse();
        return new Point(vm.getClientX(), vm.getClientY());
    }

    public Point getPressLocation() {
        var vm = inputManager.getVirtualMouse();
        return new Point(vm.getClientPressX(), vm.getClientPressY());
    }

    public long getPressTime() {
        var vm = inputManager.getVirtualMouse();
        return vm.getClientPressTime();
    }

    public boolean isPresent() {
        var vm = inputManager.getVirtualMouse();
        return vm.isClientPresent();
    }

    public boolean isPressed() {
        var vm = inputManager.getVirtualMouse();
        return vm.isClientPressed();
    }

}
