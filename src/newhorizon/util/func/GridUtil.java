package newhorizon.util.func;

import arc.Core;
import arc.graphics.Pixmap;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.struct.IntSeq;
import arc.struct.ObjectMap;
import arc.util.Time;
import mindustry.type.UnitType;
import newhorizon.util.struct.GridData;

import static mindustry.Vars.*;

public class GridUtil {
    public static final int GRID_LEN = 16;
    public static final int PX_LEN = GRID_LEN / tilesize * 2;

    public static final Point2[] samplingPixels = {new Point2(2, 2), new Point2(2, 13), new Point2(13, 13), new Point2(13, 2)};

    //key: unit inner name, value: unit grid
    public static ObjectMap<String, GridData> unitGridsMap;

    public static void init(){
        //Mindustry v8 throws away the atlas pixmaps once icons are created, so the grids can no longer be read here:
        //they are collected while the icons are created (see register), anything missing falls back to a filled grid.
        if(unitGridsMap == null) unitGridsMap = new ObjectMap<>();
    }

    /** Reads the grid of a unit from its sprite. Only works while icons are being created, call it from createIcons. */
    public static void register(UnitType type){
        if(headless) return;
        if(unitGridsMap == null) unitGridsMap = new ObjectMap<>();
        try{
            TextureRegion region = Core.atlas.find(type.name);
            if(region.found()) unitGridsMap.put(type.name, getGridData(region));
        }catch(Throwable ignored){
            //no pixmap available: gridOf falls back to a filled grid
        }
    }

    /** The grid of a unit: from its sprite if it was read in time, otherwise one as large as its icon with every cell filled. */
    public static GridData gridOf(UnitType type){
        if(unitGridsMap == null) unitGridsMap = new ObjectMap<>();
        GridData grid = unitGridsMap.get(type.name);
        if(grid == null){
            grid = filledGrid(type.fullIcon);
            unitGridsMap.put(type.name, grid);
        }
        return grid;
    }

    public static GridData filledGrid(TextureRegion region){
        int widthStep = Math.max(1, Mathf.ceil((float)region.width / GRID_LEN));
        int heightStep = Math.max(1, Mathf.ceil((float)region.height / GRID_LEN));
        IntSeq points = new IntSeq();
        points.setSize(widthStep * heightStep);
        for(int i = 0; i < points.size; i++) points.set(i, 1);

        float padLeft = (widthStep * GRID_LEN - region.width) / 2f, padBot = (heightStep * GRID_LEN - region.height) / 2f;
        return new GridData(points, widthStep, heightStep, padLeft, padBot);
    }

    public static GridData getGridData(TextureRegion region){

        int widthStep = Mathf.ceil((float) region.width / GRID_LEN);
        int heightStep = Mathf.ceil((float) region.height / GRID_LEN);

        int width = widthStep * GRID_LEN;
        int height = heightStep * GRID_LEN;

        Pixmap pixmap = Core.atlas.getPixmap(region).crop();

        float padLeft = (width - pixmap.width) / 2f, padBot = (height - pixmap.height) / 2f;
        int startLeft = -(int) padLeft, startBot = -(int) padBot;

        IntSeq tmpPoints = new IntSeq();
        tmpPoints.setSize(width * height);

        for (int x = 0; x < widthStep; x++){
            for (int y = 0; y < heightStep; y++){
                int xCoord = startLeft + x * GRID_LEN;
                int yCoord = startBot + y * GRID_LEN;

                int sampleCount = 0;
                for (Point2 point2: samplingPixels){
                    int pixel = pixmap.get(xCoord + point2.x, yCoord + point2.y);
                    //check for empty pixels, +1 if true
                    if ((pixel & 0x000000ff) == 0) sampleCount++;
                }

                if (sampleCount == 4){tmpPoints.set(y * widthStep + x, 0);}else {tmpPoints.set(y * widthStep + x, 1);}
            }
        }
        pixmap.dispose();

        return new GridData(tmpPoints, widthStep, heightStep, padLeft, padBot);
    }
}
