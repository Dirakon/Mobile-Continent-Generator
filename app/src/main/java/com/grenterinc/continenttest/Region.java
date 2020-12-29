package com.grenterinc.continenttest;

import java.util.ArrayList;

public class Region {
    public static Region[] regions;
    public Nation father = null;
    public Point l, r, u, d;
    public Point center;
    public ArrayList<Point> cells = new ArrayList<Point>();
    public ArrayList<BorderWithRegion> borders = new ArrayList<BorderWithRegion>();
    public float colorR, colorG, colorB;
    public int type = Cell.WATER;
    public int deepness = -2;
    public boolean hasRiver = false;
    public float livability;
}
