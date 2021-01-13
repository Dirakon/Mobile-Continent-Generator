package com.grenterinc.continenttest;

import android.os.Build;

import java.util.function.BiFunction;

import androidx.annotation.RequiresApi;

public class Cell {
    static final int LAND = 0, WATER = 1, DEBUG_DOT = 2, RIVER = 3;
    //  public static Cell[][] world;
    public static int sizeY, sizeX;
    private static boolean[] visibleBorderOfCell;// = false;
    private static boolean[] borderPartOfCell;// = false;
    private static boolean[] debugOfCell;// = false;
    private static int[] typeOfCell;//=WATER;
    private static int[] regionOfCell;  // = -1;

    private Cell() {

    }

    public static void initWorld(int sizeY, int sizeX) {
        Cell.sizeX = sizeX;
        Cell.sizeY = sizeY;
        int realSize = sizeX * sizeY;
        visibleBorderOfCell = new boolean[realSize];
        borderPartOfCell = new boolean[realSize];
        debugOfCell = new boolean[realSize];
        typeOfCell = new int[realSize];
        regionOfCell = new int[realSize];
        for (int i = 0; i < realSize; ++i) {
            visibleBorderOfCell[i] = false;
            borderPartOfCell[i] = false;
            debugOfCell[i] = false;
            typeOfCell[i] = WATER;
            regionOfCell[i] = -1;
        }
    }

    public static int getIdByCoords(int y, int x) {
        return y * sizeX + x;
    }

    public static boolean getVisibleBorder(int id) {
        return visibleBorderOfCell[id];
    }

    public static boolean getBorderPartOfCell(int id) {
        return borderPartOfCell[id];
    }

    public static boolean getDebugOfCell(int id) {
        return debugOfCell[id];
    }

    public static int getTypeOfCell(int id) {
        return typeOfCell[id];
    }

    public static int getRegionOfCell(int id) {
        return regionOfCell[id];
    }

    public static void setVisibleBorder(int id, boolean data) {
        visibleBorderOfCell[id] = data;
    }

    public static void setBorderPartOfCell(int id, boolean data) {
        borderPartOfCell[id] = data;
    }

    public static void setDebugOfCell(int id, boolean data) {
        debugOfCell[id] = data;
    }

    public static void setTypeOfCell(int id, int data) {
        typeOfCell[id] = data;
    }

    public static void setRegionOfCell(int id, int data) {
        regionOfCell[id] = data;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void goingAroundWithFunc9(BiFunction<Integer, Integer, Void> func, int y, int x) {
        //Going through all the neighbouring cells (INCLUDING THE CENTRAL ONE (y,x)!!!)with lambda function with some logic
        //Some conservative checking
        if (x < 0)
            x += sizeX;
        else if (x >= sizeX)
            x -= sizeX;

        //Classic grid stuff
        int start_y = y == 0 ? y : y - 1;
        int end_y = y == sizeY - 1 ? y : y + 1;
        int start_x = x == 0 ? x : x - 1;
        int end_x = x == sizeX - 1 ? x : x + 1;
        if (x == 0) {
            func.apply(y, sizeX - 1);
            if (y != 0)
                func.apply(y - 1, sizeX - 1);
            if (y != sizeY - 1)
                func.apply(y + 1, sizeX - 1);
        } else if (x == sizeX - 1) {
            func.apply(y, 0);
            if (y != 0)
                func.apply(y - 1, 0);
            if (y != sizeY - 1)
                func.apply(y + 1, 0);
        }
        for (int _y = start_y; _y <= end_y; ++_y)
            for (int _x = start_x; _x <= end_x; ++_x) {
                func.apply(_y, _x);
            }
    }
}
