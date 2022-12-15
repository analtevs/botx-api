package dax_api.walker.utils.camera;

import dax_api.Ctx;

import rsb_api.methods.Camera;
import net.runelite.rsb.util.StdRandom;
import rsb_api.wrappers.common.Positionable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static dax_api.walker.utils.camera.CameraCalculations.getRotationToTile;

public class AsynchronousCamera {

    private static AsynchronousCamera instance = null;
    private ExecutorService executorService;
    private Future angleTask, rotationTask;

    private static AsynchronousCamera getInstance() {
        return instance != null ? instance : (instance = new AsynchronousCamera());
    }

    private AsynchronousCamera() {
        executorService = Executors.newFixedThreadPool(2);
    }

    public static Future focus(Positionable positionable){
        Future rotation = setCameraRotation(getRotationToTile(positionable), 0);
        Future angle = setCameraAngle(CameraCalculations.getAngleToTile(positionable), 0);
        return rotation;
    }

    public static synchronized Future setCameraAngle(int angle, int tolerance){
        if (getInstance().angleTask != null && !getInstance().angleTask.isDone()){
            return null;
        }
        Ctx.ctx.camera.setRotationMethod(Camera.ROTATION_METHOD.ONLY_KEYS);
            return getInstance().angleTask = getInstance().executorService.submit(() -> Ctx.ctx.camera.setPitch(
		            CameraCalculations.normalizeAngle(angle + StdRandom.uniform(-tolerance, tolerance))));
    }

    public static synchronized Future setCameraRotation(int degrees, int tolerance){
        if (getInstance().rotationTask != null && !getInstance().rotationTask.isDone()){
            return null;
        }
        Ctx.ctx.camera.setRotationMethod(Camera.ROTATION_METHOD.ONLY_KEYS);
        return getInstance().rotationTask = getInstance().executorService.submit(() -> Ctx.ctx.camera.setAngle(
		        CameraCalculations.normalizeRotation(degrees + StdRandom.uniform(-tolerance, tolerance))));
    }

}
