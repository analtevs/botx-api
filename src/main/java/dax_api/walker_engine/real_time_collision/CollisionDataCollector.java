package dax_api.walker_engine.real_time_collision;

import dax_api.Ctx;
import rsb_api.wrappers.subwrap.WalkerTile;


public class CollisionDataCollector {

    public static void generateRealTimeCollision() {
        RealTimeCollisionTile.clearMemory();

        WalkerTile playerPosition = new WalkerTile(new WalkerTile(Ctx.ctx.players.getMyPlayer().getLocation()));
		int[][] collisionData = Ctx.ctx.walking.getCollisionData();

        if (collisionData == null) {
            return;
        }

        for (int i = 0; i < collisionData.length; i++) {
            for (int j = 0; j < collisionData[i].length; j++) {
                WalkerTile localTile = new WalkerTile(Ctx.ctx, i, j, playerPosition.getPlane(), WalkerTile.TYPES.SCENE);
                WalkerTile worldTile = localTile.toWorldTile();

                RealTimeCollisionTile.create(worldTile.getX(), worldTile.getY(), worldTile.getPlane(), collisionData[i][j]);
            }
        }
    }

    public static void updateRealTimeCollision(){
        WalkerTile playerPosition = new WalkerTile(new WalkerTile(Ctx.ctx.players.getMyPlayer().getLocation()));
		int[][] collisionData = Ctx.ctx.walking.getCollisionData();
        if (collisionData == null) {
            return;
		}

        for (int i = 0; i < collisionData.length; i++) {
            for (int j = 0; j < collisionData[i].length; j++) {
                WalkerTile localTile = new WalkerTile(Ctx.ctx, i, j, playerPosition.getPlane(), WalkerTile.TYPES.SCENE);
                WalkerTile worldTile = localTile.toWorldTile();


				RealTimeCollisionTile realTimeCollisionTile = RealTimeCollisionTile.get(worldTile.getX(), worldTile.getY(), worldTile.getPlane());
                if (realTimeCollisionTile != null){
                    realTimeCollisionTile.setCollisionData(collisionData[i][j]);
                } else {
                    RealTimeCollisionTile.create(worldTile.getX(), worldTile.getY(), worldTile.getPlane(), collisionData[i][j]);
                }
            }
        }
    }

}
