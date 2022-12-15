package dax_api.walker.utils.camera;

import dax_api.Ctx;

import rsb_api.wrappers.RSCharacter;
import rsb_api.wrappers.common.Positionable;
import rsb_api.wrappers.subwrap.WalkerTile;

public class CameraCalculations {

    public static int normalizeAngle(int angle) {
        return Calculations.limitRange(angle, 0, 100);
    }

    public static int normalizeRotation(int rotation) {
        return rotation % 360;
    }

    public static int distanceBetweenTwoAngles(int alpha, int beta) {
		// This is either the distance or 360 - distance
        int phi = Math.abs(beta - alpha) % 360;
        return phi > 180 ? 360 - phi : phi;
    }

    public static int getAngleToTile(Positionable tile) {
        return 100 - (int) (Math.min(new WalkerTile(new WalkerTile(Ctx.ctx.players.getMyPlayer().getLocation())).distanceToDouble(tile), 15) * 4);
    }

    public static int getRotationToTile(RSCharacter target) {
        WalkerTile location = new WalkerTile(target.getLocation());
        RSCharacter.DIRECTION direction = target.getDirectionFacing();
        int cameraRotation = Ctx.ctx.camera.getCharacterAngle(target);
        switch (direction) {
            case N:
                cameraRotation = Ctx.ctx.camera.getTileAngle(location.translate(0, 1));
                break;
            case E:
                cameraRotation = Ctx.ctx.camera.getTileAngle(location.translate(1, 0));
                break;
            case S:
                cameraRotation = Ctx.ctx.camera.getTileAngle(location.translate(0, -1));
                break;
            case W:
                cameraRotation = Ctx.ctx.camera.getTileAngle(location.translate(-1, 0));
                break;
            case NE:
                cameraRotation = Ctx.ctx.camera.getTileAngle(location.translate(1, 1));
                break;
            case NW:
                cameraRotation = Ctx.ctx.camera.getTileAngle(location.translate(-1, 1));
                break;
            case SE:
                cameraRotation = Ctx.ctx.camera.getTileAngle(location.translate(1, -1));
                break;
            case SW:
                cameraRotation = Ctx.ctx.camera.getTileAngle(location.translate(-1, -1));
                break;
        }

        int currentCameraRotation = Ctx.ctx.camera.getAngle();
        return cameraRotation + (distanceBetweenTwoAngles(cameraRotation + 45, currentCameraRotation) < distanceBetweenTwoAngles(cameraRotation - 45, currentCameraRotation) ? 45 : -45);
    }

    public static int getRotationToTile(Positionable target) {
        int cameraRotation = Ctx.ctx.camera.getTileAngle(target.getLocation());
        int currentCameraRotation = Ctx.ctx.camera.getAngle();
        return cameraRotation + (distanceBetweenTwoAngles(cameraRotation + 45, currentCameraRotation) < distanceBetweenTwoAngles(cameraRotation - 45, currentCameraRotation) ? 45 : -45);
    }

}
