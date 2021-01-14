package com.grenterinc.continenttest;

import android.graphics.Color;
import android.os.Build;

import androidx.annotation.RequiresApi;

//Terrain description
public class TerrainType {
    public static final int PLANE = -1, BEACH = -2, SNOW = -3, SPAWNABLETYPES = 3;
    public static TerrainType[] terrainTypes = new TerrainType[]{
            new TerrainType(    //PLANE (everything is PLANE by default)
                    1,    //Chance To Spawn (0-100)
                    1,    //Min Size (in regions)
                    1,//Max Size (in regions)
                    true, //Can river appear in this region
                    true,//Can this region become different or can others become this in terrain smoothing
                    true,//Can others become this in terrain smoothing
                    0.5f,//if (excluding water) at least that part of neighbouring regions is the same, don't change - else change;
                    0,    //min MINIMAL distance from equator
                    0,  //max MINIMAL distance from equator
                    0,    //min MAXIMAL distance from equator
                    0,  //max MAXIMAL distance from equator
                    2.5f, //livablity
                    0, 1, 0, //rgb
                    1,    //river livability increase
                    "Planes"    //name
            ),
            new TerrainType(    //BEACH (spawns in special way)

                    100,    //Chance To Spawn (0-100)
                    1,    //Min Size (in regions)
                    1,//Max Size (in regions)
                    true, //Can river appear in this region
                    false,//Can this region become different in terrain smoothing
                    false,//Can others become this in terrain smoothing
                    0.5f,//if (excluding water) at least that part of neighbouring regions is the same, don't change - else change;
                    0,    //min MINIMAL distance from equator
                    0,  //max MINIMAL distance from equator
                    0,    //min MAXIMAL distance from equator
                    0,  //max MAXIMAL distance from equator
                    3, //livablity
                    1, 1, 0.2f, //rgb
                    0.4f,    //river livability increase
                    "Beach"    //name
            ),
            new TerrainType(    //ARCTIC (spawns in special way)
                    100,    //Chance To Spawn (0-100)
                    1,    //Min Size (in regions)
                    1,//Max Size (in regions)
                    false, //Can river appear in this region
                    false,//Can this region become different or can others become this in terrain smoothing
                    true,//Can others become this in terrain smoothing
                    0.5f,//if (excluding water) at least that part of neighbouring regions is the same, don't change - else change;
                    0.8f,    //min MINIMAL distance from equator
                    0.9f,  //max MINIMAL distance from equator
                    1,    //min MAXIMAL distance from equator
                    1,  //max MAXIMAL distance from equator
                    0, //livablity
                    0.8f, 1, 1, //rgb
                    0.2f,    //river livability increase
                    "Arctic"    //name

            ),
            new TerrainType(    //MOUNTAIN

                    3,    //Chance To Spawn (0-100)
                    1,    //Min Size (in regions)
                    4,//Max Size (in regions)
                    true, //Can river appear in this region
                    false,//Can this region become different or can others become this in terrain smoothing
                    false,//Can others become this in terrain smoothing
                    0,//if (excluding water) at least that part of neighbouring regions is the same, don't change - else change;
                    0,    //min MINIMAL distance from equator
                    0,  //max MINIMAL distance from equator
                    1,    //min MAXIMAL distance from equator
                    1,  //max MAXIMAL distance from equator
                    0.5f, //livablity
                    0.75f, 0.5f, 0.25f, //rgb
                    0.4f,    //river livability increase
                    "Mountains"    //name

            ),
            new TerrainType(    //DESERT
                    6,    //Chance To Spawn (0-100)
                    4,    //Min Size (in regions)
                    8,//Max Size (in regions)
                    false, //Can river appear in this region
                    true,//Can this region become different or can others become this in terrain smoothing
                    true,//Can others become this in terrain smoothing
                    0.5f,//if (excluding water) at least that part of neighbouring regions is the same, don't change - else change;
                    0,    //min MINIMAL distance from equator
                    0,  //max MINIMAL distance from equator
                    0.4f,    //min MAXIMAL distance from equator
                    0.6f,  //max MAXIMAL distance from equator
                    1, //livablity
                    1, 0.95f, 0, //rgb
                    0.5f,    //river livability increase
                    "Desert"    //name

            ),
            new TerrainType(    //TROPICAL FOREST
                    6,    //Chance To Spawn (0-100)
                    4,    //Min Size (in regions)
                    8,//Max Size (in regions)
                    true, //Can river appear in this region
                    true,//Can this region become different or can others become this in terrain smoothing
                    true,//Can others become this in terrain smoothing
                    0.5f,//if (excluding water) at least that part of neighbouring regions is the same, don't change - else change;
                    0,    //min MINIMAL distance from equator
                    0,  //max MINIMAL distance from equator
                    0.4f,    //min MAXIMAL distance from equator
                    0.6f,  //max MAXIMAL distance from equator
                    1.5f, //livablity
                    0, 0.7f, 0, //rgb
                    0.4f,    //river livability increase
                    "Tropical forest"    //name

            ),
            new TerrainType(    //FOREST
                    6,    //Chance To Spawn (0-100)
                    4,    //Min Size (in regions)
                    8,//Max Size (in regions)
                    true, //Can river appear in this region
                    true,//Can this region become different or can others become this in terrain smoothing
                    true,//Can others become this in terrain smoothing
                    0.5f,//if (excluding water) at least that part of neighbouring regions is the same, don't change - else change;
                    0,    //min MINIMAL distance from equator
                    0,  //max MINIMAL distance from equator
                    1,    //min MAXIMAL distance from equator
                    1,  //max MAXIMAL distance from equator
                    1.9f, //livablity
                    0, 0.85f, 0, //rgb
                    1,    //river livability increase
                    "Forest"    //name
            )};
    public int chanceToSpawn;
    public int minSize, maxSize;
    public boolean canRiverSpawn, canBeSmoothed, canSmoothOthers;
    public float riverLivabilityIncrease;
    public float smoothingTreshold;
    public float minMinEquatorDistance, maxMinEquatorDistance;
    public float minMaxEquatorDistance, maxMaxEquatorDistance;
    public float livability;
    public float r, g, b;
    public String name;

    @RequiresApi(api = Build.VERSION_CODES.O)
    public int getColor() {
        return Color.rgb(r, g, b);
    }

    public TerrainType(int chanceToSpawn, int minSize, int maxSize, boolean canRiverSpawn, boolean canBeSmoothed, boolean canSmoothOthers, float smoothingTreshold, float minMinEquatorDistance, float maxMinEquatorDistance, float minMaxEquatorDistance, float maxMaxEquatorDistance, float livability, float r, float g, float b, float riverLivabilityIncrease, String name) {
        this.chanceToSpawn = chanceToSpawn;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.canRiverSpawn = canRiverSpawn;
        this.canBeSmoothed = canBeSmoothed;
        this.canSmoothOthers = canSmoothOthers;
        this.smoothingTreshold = smoothingTreshold;
        this.minMinEquatorDistance = minMinEquatorDistance;
        this.maxMinEquatorDistance = maxMinEquatorDistance;
        this.minMaxEquatorDistance = minMaxEquatorDistance;
        this.maxMaxEquatorDistance = maxMaxEquatorDistance;
        this.livability = livability;
        this.r = r;
        this.g = g;
        this.b = b;
        this.riverLivabilityIncrease = riverLivabilityIncrease;
        this.name = name;
    }

    public static TerrainType getTerrainTypeByDeepness(int deepness) {
        return terrainTypes[-deepness - 1];
    }

}