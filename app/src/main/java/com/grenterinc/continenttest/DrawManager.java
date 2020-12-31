package com.grenterinc.continenttest;

import android.graphics.Color;
import android.os.Build;

import java.util.ArrayList;
import java.util.Stack;

import androidx.annotation.RequiresApi;

import static com.grenterinc.continenttest.Cell.DEBUG_DOT;
import static com.grenterinc.continenttest.Cell.LAND;
import static com.grenterinc.continenttest.Cell.RIVER;
import static com.grenterinc.continenttest.Cell.WATER;
import static com.grenterinc.continenttest.Cell.world;
import static com.grenterinc.continenttest.Generator.moreDiv;
import static com.grenterinc.continenttest.Region.regions;
import static com.grenterinc.continenttest.TerrainType.getTerrainTypeByDeepness;

@RequiresApi(api = Build.VERSION_CODES.O)
public class DrawManager {
    public static final int DRAW_TERRAIN = 0, DRAW_REGIONS = 1, DRAW_COUNTRIES = 2, DRAW_LIVABILITY = 3;
    public static int drawType = DRAW_COUNTRIES;
    public static Stack<Point> cellsForUpdate = new Stack<Point>();
    public static Stack<Region> regionsForUpdate = new Stack<Region>();
    public static Stack<ArrayList<BorderCell>> borderForUpdate = new Stack<ArrayList<BorderCell>>();
    public static boolean updateAll = false, drawBorders = true;
    public static int horizontalOffset = 0;
    public static int debugColor = Color.RED, riverColor = Color.rgb(0.25f, 0.25f, 1f),
            borderColor = Color.BLACK;

    private static void drawCell(int x, int y, int sizeX) {
        int color = 0;
        if (world[y][x].debug) {
            color = debugColor;
        } else if (world[y][x].type == RIVER && drawType != DRAW_REGIONS) {
            color = riverColor;
        } else if (world[y][x].visibleBorder && drawBorders) {
            color = borderColor;
        } else if (world[y][x].type == DEBUG_DOT || world[y][x].region == -1 || drawType == DRAW_TERRAIN || (drawType == DRAW_COUNTRIES && (regions[world[y][x].region].father == null))) {
            int type = world[y][x].type;
            switch (type) {
                case WATER:
                    color = Color.rgb(0f, 0f, (float) (1 - 0.1 * Generator.moreDiv * regions[world[y][x].region].deepness));
                    break;
                case LAND:
                    color = getTerrainTypeByDeepness(regions[world[y][x].region].deepness).getColor();
                    break;
            }
        } else if (drawType == DRAW_REGIONS) {
            int regId = world[y][x].region;
            color = Color.rgb(regions[regId].colorR, regions[regId].colorG, regions[regId].colorB);

        } else if (drawType == DRAW_COUNTRIES) {

            int regId = world[y][x].region;
            Region reg = regions[regId];
            color = Color.rgb(reg.father.colorR, reg.father.colorG, reg.father.colorB);
        } else {
            int regId = world[y][x].region;
            Region reg = regions[regId];
            if (world[y][x].type == WATER) {
                color = Color.rgb(0, 0, (float) (1 - 0.1 * moreDiv * regions[world[y][x].region].deepness));
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
        } else {
            while (!cellsForUpdate.empty()) {
                Point point = cellsForUpdate.pop();
                drawCell(point.x, point.y, sizeX);
            }
            while (!regionsForUpdate.empty()) {
                Region reg = regionsForUpdate.pop();
                for (Point point : reg.cells) {
                    drawCell(point.x, point.y, sizeX);
                }
            }
            while (!borderForUpdate.empty()) {
                ArrayList<BorderCell> bord = borderForUpdate.pop();
                for (BorderCell point : bord) {
                    drawCell(point.x, point.y, sizeX);
                }
            }
        }
    }


}
