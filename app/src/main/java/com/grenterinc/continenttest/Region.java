package com.grenterinc.continenttest;

import java.util.LinkedList;

public class Region {
    public static Region[] regions;
    public static Region[] landRegions;     // Exists because most of the time you need only land regions
    public Nation father = null;
    public Point l, r, u, d;
    public Point center;
    public LinkedList<Point> cells = new LinkedList<Point>();
    public LinkedList<BorderWithRegion> borders = new LinkedList<BorderWithRegion>();
    public float colorR, colorG, colorB;
    public int type = Cell.WATER;
    public int deepness = -2;
    public boolean hasRiver = false;
    public float livability;

    public TerrainType getTerrainType() {
        return TerrainType.getTerrainTypeByDeepness(deepness);
    }
}
