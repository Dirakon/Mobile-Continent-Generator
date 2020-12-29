package com.grenterinc.continenttest;

import android.os.Build;

import java.util.function.BiFunction;

import androidx.annotation.RequiresApi;

public class Cell {
    static final int LAND = 0, WATER = 1, DEBUG_DOT = 2, RIVER = 3;
    public static Cell[][] world;
    public boolean visibleBorder = false;
    public boolean borderPart = false;
    public boolean debug = false;
    public int type;
    public int region = -1;

    public Cell() {
        type = WATER;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void goingAroundWithFunc9(BiFunction<Integer, Integer, Void> func, int y, int x) {
        int sizeY = world.length, sizeX = world[0].length;
        if (x < 0)
            x += sizeX;
        else if (x >= sizeX)
            x -= sizeX;
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
