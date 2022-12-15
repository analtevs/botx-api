package dax_api.walker.utils.camera;

import dax_api.Ctx;
import dax_api.walker.utils.AccurateMouse;
import net.runelite.rsb.util.StdRandom;
import rsb_api.wrappers.common.Positionable;

import java.awt.*;

import static dax_api.walker.utils.camera.CameraCalculations.distanceBetweenTwoAngles;

public class DaxCamera {

    private static float PIXEL_TO_ANGLE_RATIO = 2.253731343283582F, PIXEL_TO_ROTATION_RATIO = 2.966666666666667F;

    public static void focus(Positionable positionable) {
        positionCamera(CameraCalculations.getAngleToTile(positionable),
					   CameraCalculations.getRotationToTile(positionable));
    }

    public static void positionCamera(int angle, int rotation) {
        if (!CameraAction.isMiddleMouseCameraOn()){
            return;
        }

        int currentAngle = Ctx.ctx.camera.getPitch();
		int currentRotation = Ctx.ctx.camera.getAngle();

        int cameraAngleDifference = angle - currentAngle;

		// man java wtf?
        int cameraRotationDifference = distanceBetweenTwoAngles(currentRotation, rotation), rotationDirection;

		var normalizeRot = CameraCalculations.normalizeRotation(currentRotation + cameraRotationDifference);
        if (normalizeRot == rotation) {
			//TURN RIGHT
            rotationDirection = -1;
        } else {
            rotationDirection = 1;
        }

        Point point = new Point(Ctx.ctx.mouse.getLocation().getX(),
								Ctx.ctx.mouse.getLocation().getY());

        if (!getGameScreen().contains(point)) {
            Ctx.ctx.mouse.move(AccurateMouse.getRandomPoint((getGameScreen())));
        }

        point = new Point(Ctx.ctx.mouse.getLocation().getX(), Ctx.ctx.mouse.getLocation().getY());
        Point startingPoint = point;
        Point endingPoint = new Point(startingPoint);

        int dx = rotationDirection * cameraRotationDifference;
        int dy = cameraAngleDifference;

        endingPoint.translate(rotationToPixel(dx), angleToPixel(dy));

        Ctx.ctx.mouse.drag((int) endingPoint.getX(), (int) endingPoint.getY());
    }

    public static Rectangle getGameScreen(){
        return new Rectangle(0, 0, 765, 503);
    }

    private static int rotationToPixel(int rotation){
        return (int) (rotation * PIXEL_TO_ROTATION_RATIO);
    }

    private static int angleToPixel(int angle){
        return (int) (angle * PIXEL_TO_ANGLE_RATIO);
    }

    private static Point generatePoint(Rectangle rectangle){
        return new Point(StdRandom.uniform(rectangle.x, rectangle.x + rectangle.width),
						 StdRandom.uniform(rectangle.y, rectangle.y + rectangle.height));
    }
}
