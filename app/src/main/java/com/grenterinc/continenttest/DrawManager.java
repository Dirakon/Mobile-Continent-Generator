package com.grenterinc.continenttest;

import android.graphics.Color;
import android.os.Build;

import java.util.LinkedList;
import java.util.Stack;

import androidx.annotation.RequiresApi;

import static com.grenterinc.continenttest.Cell.DEBUG_DOT;
import static com.grenterinc.continenttest.Cell.LAND;
import static com.grenterinc.continenttest.Cell.RIVER;
import static com.grenterinc.continenttest.Cell.WATER;
import static com.grenterinc.continenttest.Generator.moreDiv;
import static com.grenterinc.continenttest.Region.regions;
import static com.grenterinc.continenttest.TerrainType.getTerrainTypeByDeepness;

@RequiresApi(api = Build.VERSION_CODES.O)
public class DrawManager {
    public static final int DRAW_TERRAIN = 0, DRAW_REGIONS = 1, DRAW_COUNTRIES = 2, DRAW_LIVABILITY = 3;
    public static int drawType = DRAW_TERRAIN;
    public static Stack<Point> cellsForUpdate = new Stack<Point>();
    public static Stack<Region> regionsForUpdate = new Stack<Region>();
    public static Stack<LinkedList<BorderCell>> borderForUpdate = new Stack<LinkedList<BorderCell>>();
    public static boolean updateAll = false, drawBorders = true;
    public static int horizontalOffset = 0;
    public static int debugColor = Color.RED, riverColor = Color.rgb(0.25f, 0.25f, 1f),
            borderColor = Color.BLACK;

    private static void drawCell(int x, int y, int sizeX) {
        int id = Cell.getIdByCoords(y, x);
        int color = 0;

        if (Cell.getDebugOfCell(id)) {
            color = debugColor;
        } else if (Cell.getTypeOfCell(id) == RIVER && drawType != DRAW_REGIONS) {
            color = riverColor;
        } else if (Cell.getVisibleBorder(id) && drawBorders) {
            color = borderColor;
        } else if (Cell.getTypeOfCell(id) == DEBUG_DOT || Cell.getRegionOfCell(id) == -1 || drawType == DRAW_TERRAIN || (drawType == DRAW_COUNTRIES && (regions[Cell.getRegionOfCell(id)].father == null))) {
            int type = Cell.getTypeOfCell(id);
            switch (type) {
                case WATER:
                    color = Color.rgb(0f, 0f, (float) (1 - 0.1 * Generator.moreDiv * regions[Cell.getRegionOfCell(id)].deepness));
                    break;
                case LAND:
                    color = getTerrainTypeByDeepness(regions[Cell.getRegionOfCell(id)].deepness).getColor();
                    break;
            }
        } else if (drawType == DRAW_REGIONS) {
            int regId = Cell.getRegionOfCell(id);
            color = Color.rgb(regions[regId].colorR, regions[regId].colorG, regions[regId].colorB);

        } else if (drawType == DRAW_COUNTRIES) {

            int regId = Cell.getRegionOfCell(id);
            Region reg = regions[regId];
            color = Color.rgb(reg.father.colorR, reg.father.colorG, reg.father.colorB);
        } else {
            int regId = Cell.getRegionOfCell(id);
            Region reg = regions[regId];
            if (Cell.getTypeOfCell(id) == WATER) {
                color = Color.rgb(0, 0, (float) (1 - 0.1 * moreDiv * regions[Cell.getRegionOfCell(id)].deepness));
            } else {
                color = Color.rgb(1.0f - reg.livability / 3.5f, reg.livability / 3.5f, 0f);

            }
        }
        MyDrawer.bitmap.setPixel((x + horizontalOffset) % sizeX, y, color);
    }


    public static void Draw(int sizeY, int sizeX) {
        MainActivity.Debug("DRAWING");
        if (updateAll) {
            updateAll = false;
            for (int y = 0; y < sizeY; ++y) {
                for (int x = 0; x < sizeX; ++x) {
                    drawCell(x, y, sizeX);
                }
            }
            MyDrawer.singelton.invalidate();
        } else {
            while (!cellsForUpdate.empty()) {
                Point point = cellsForUpdate.pop();
                drawCell(point.x, point.y, sizeX);
                MyDrawer.singelton.invalidate();
            }
            while (!regionsForUpdate.empty()) {
                Region reg = regionsForUpdate.pop();
                for (Point point : reg.cells) {
                    drawCell(point.x, point.y, sizeX);
                }
                MyDrawer.singelton.invalidate();
            }
            while (!borderForUpdate.empty()) {
                LinkedList<BorderCell> bord = borderForUpdate.pop();
                for (BorderCell point : bord) {
                    drawCell(point.x, point.y, sizeX);
                }
                MyDrawer.singelton.invalidate();
            }
        }
    }


}
