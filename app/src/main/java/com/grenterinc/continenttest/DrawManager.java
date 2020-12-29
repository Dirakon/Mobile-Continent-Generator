package com.grenterinc.continenttest;

import java.util.ArrayList;
import java.util.Stack;

public class DrawManager {
    public static final int DRAW_TERRAIN = 0, DRAW_REGIONS = 1, DRAW_COUNTRIES = 2, DRAW_LIVABILITY = 3;
    public static int drawType = DRAW_COUNTRIES;
    public static Stack<Point> cellsForUpdate = new Stack<Point>();
    public static Stack<Region> regionsForUpdate = new Stack<Region>();
    public static Stack<ArrayList<BorderCell>> borderForUpdate = new Stack<ArrayList<BorderCell>>();
    public boolean updateAll = false, drawBorders = true;
}
