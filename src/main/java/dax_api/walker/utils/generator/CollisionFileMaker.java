package dax_api.walker.utils.generator;

import dax_api.Ctx;
import dax_api.walker_engine.local_pathfinding.AStarNode;
import net.runelite.rsb.internal.globval.GlobalConfiguration;
import rsb_api.wrappers.subwrap.WalkerTile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class CollisionFileMaker {

    public static void getCollisionData() {
        try {
            int[][] collisionData = Ctx.ctx.walking.getCollisionData();
            if (collisionData == null)
                return;

            int baseX = Ctx.ctx.game.getBaseX();
            int baseY = Ctx.ctx.game.getBaseY();
            int baseZ = new WalkerTile(Ctx.ctx.players.getMyPlayer().getLocation()).getWorldLocation().getPlane();
            File file = new File(baseX + "x" + baseY + "x" + baseZ + ".cdata");

            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
            for (int x = 0; x < collisionData.length; x++) {
                for (int y = 0; y < collisionData[x].length; y++) {
                    int flag = collisionData[x][y];
                    WalkerTile tile = new WalkerTile(Ctx.ctx, x, y, baseZ, WalkerTile.TYPES.SCENE).toWorldTile();
                    CollisionTile collisionTile = new CollisionTile(
                            tile.getX(), tile.getY(), tile.getPlane(),
                            AStarNode.blockedNorth(flag),
                            AStarNode.blockedEast(flag),
                            AStarNode.blockedSouth(flag),
                            AStarNode.blockedWest(flag),
                            !AStarNode.isWalkable(flag),
                            false,
                            !AStarNode.isInitialized(flag));
                    bufferedWriter.write(collisionTile.toString());
                    bufferedWriter.newLine();
                }
            }
            bufferedWriter.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

}
