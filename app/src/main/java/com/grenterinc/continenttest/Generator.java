package com.grenterinc.continenttest;

import android.os.Build;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import androidx.annotation.RequiresApi;

import static com.grenterinc.continenttest.Cell.world;

public class Generator {

    private static final int worldSmoothingTreshold = 4;//1...7, but recommended to leave at 4.
    public static int moreDiv = 1; //How much seas are more rare than land regions. The more number is, less sea regions there are.
    private static int cwspMAX = 5; //1...8, really takes much memory, but (probably) the effect is worth it? Can be around 3 if there are memory problems.

    @RequiresApi(api = Build.VERSION_CODES.N)
    static private void AddToAllArrays(int x, int y, CoolWSPointer[][] dArrayOfList,
                                       PointNNumInCWSP[] newAgeList, int ptr,
                                       BiFunction<Cell, Cell, Boolean> checkFunc,
                                       BiFunction<Cell, Cell, Void> doFunc) {
        AddToAllArrays(x, y, dArrayOfList, newAgeList, ptr, checkFunc, doFunc, null);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    static private void AddToAllArrays(int x, int y, CoolWSPointer[][] dArrayOfList,
                                       PointNNumInCWSP[] newAgeList, Integer ptr,
                                       BiFunction<Cell, Cell, Boolean> checkFunc,
                                       BiFunction<Cell, Cell, Void> doFunc,
                                       Cell whoAddedThis) {


        //Clear all pointer to this block.
        for (int i = 0; i < dArrayOfList[y][x].ptr; ++i) {
            int curPtr = dArrayOfList[y][x].arr[i];
            --ptr;
            dArrayOfList[newAgeList[ptr].point.y][newAgeList[ptr].point.x].arr[newAgeList[ptr].num] = curPtr;
            newAgeList[curPtr] = newAgeList[ptr];
        }
        dArrayOfList[y][x].ptr = 0;

        //We make this block WHATEVER TYPE WE ASKED FOR

        doFunc.apply(world[y][x], whoAddedThis);

        //Atomic copy of ptr

        AtomicReference<Integer> timelyPtr = new AtomicReference<Integer>(ptr);

        //We add pointer to new NON-(THIS TYPE) neighbours

        BiFunction<Integer, Integer, Void> func = (Integer yf, Integer xf) -> {
            if (checkFunc.apply(world[yf][xf], world[y][x]) && dArrayOfList[yf][xf].ptr != cwspMAX) {
                newAgeList[timelyPtr.get()].point.x = xf;
                newAgeList[timelyPtr.get()].point.y = yf;
                newAgeList[timelyPtr.get()].num = dArrayOfList[yf][xf].ptr;
                newAgeList[timelyPtr.get()].whoAdded = world[y][x];
                dArrayOfList[yf][xf].arr[dArrayOfList[yf][xf].ptr++] = timelyPtr.get();
                timelyPtr.set(timelyPtr.get() + 1);
            }
            return null;
        };
        Cell.goingAroundWithFunc9(func, y, x);

        //Reset ptr
        ptr = timelyPtr.get();
    }

    static private void clearPTRs(CoolWSPointer[][] dArrayOfLists, Integer newAgePtr, int sizeY, int sizeX) {
        newAgePtr = 0;
        for (int i = 0; i < sizeY; ++i) {
            for (int x = 0; x < sizeX; ++x) {
                dArrayOfLists[i][x].ptr = 0;
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    static private void GrowSeeds(ArrayList<Point> seedsList, GenerationInniter inniter, CoolWSPointer[][] dArrayOfLists, Integer newAgePtr, PointNNumInCWSP[] newAgeList, int sizeY, int sizeX) {
        BiFunction<Cell, Cell, Boolean> boolIfOther = (Cell cell, Cell whoAdded) -> {
            return (cell.type == inniter.typeToTransformFrom);
        };
        BiFunction<Cell, Cell, Void> makeThis = (Cell cell, Cell whoAdded) -> {
            cell.type = inniter.typeToTransformInto;
            return null;
        };
        for (Point seed : seedsList) {
            clearPTRs(dArrayOfLists, newAgePtr, sizeY, sizeX);
            int landAmount = (int) (Std.inBetweenTwoFloats(inniter.minProc, inniter.maxProc) * (sizeX * sizeY));
            int orx = seed.x;
            int ory = seed.y;
            world[ory][orx].type = inniter.typeToTransformFrom;
            AddToAllArrays(orx, ory, dArrayOfLists, newAgeList, newAgePtr, boolIfOther, makeThis);

            for (int curLand = 0; curLand < landAmount && newAgePtr != 0; ++curLand) {
                int pol = new Random().nextInt(newAgePtr);
                AddToAllArrays(newAgeList[pol].point.x, newAgeList[pol].point.y, dArrayOfLists, newAgeList, newAgePtr, boolIfOther, makeThis);

            }
        }
    }

    static private void PlaceSeeds(ArrayList<Point> seedsList, GenerationInniter inniter) {
        //  int cura = pointsList.size();
        int continentSeeds = Std.inBetweenTwoInts(inniter.minSeeds, inniter.maxSeeds);
        for (int i = 0; i < continentSeeds; ++i) {
            int y, x;
            do {
                y = new Random().nextInt(world.length);
                x = new Random().nextInt(world[0].length);
            } while (world[y][x].type == inniter.typeToTransformInto);
            world[y][x].type = inniter.typeToTransformInto;
            Point p = new Point();
            p.y = y;
            p.x = x;
            seedsList.add(p);
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private static void Smoothing(CoolWSPointer[][] dArrayOfLists, Integer newAgePtr, PointNNumInCWSP[] newAgeList, int sizeY, int sizeX) {
        int safeSWCP = cwspMAX;
        cwspMAX = 8;

        clearPTRs(dArrayOfLists, newAgePtr, sizeY, sizeX);
        MainActivity.Debug("Smoothing mapping...\n");
        for (int y = 0; y < sizeY; ++y) {
            for (int x = 0; x < sizeX; ++x) {
                AtomicReference<Integer> counterRef = new AtomicReference<Integer>(0);
                int finalY = y;
                int finalX = x;
                BiFunction<Integer, Integer, Void> func = (Integer yf, Integer xf) -> {
                    if (world[yf][xf].type != world[finalY][finalX].type) {
                        counterRef.set(counterRef.get() + 1);
                    }
                    return null;
                };
                Cell.goingAroundWithFunc9(func, y, x);
                int counter = counterRef.get();
                if (counter > 0 && dArrayOfLists[y][x].ptr != cwspMAX) {
                    newAgeList[newAgePtr].point.x = x;
                    newAgeList[newAgePtr].point.y = y;
                    newAgeList[newAgePtr].num = dArrayOfLists[y][x].ptr;
                    dArrayOfLists[y][x].arr[dArrayOfLists[y][x].ptr++] = newAgePtr++;
                }
            }
        }
        MainActivity.Debug("Actual smoothing...\n");
        while (newAgePtr != 0) {
            int ran = new Random().nextInt(newAgePtr);
            AtomicReference<Integer> counterRef = new AtomicReference<Integer>(0);

            BiFunction<Integer, Integer, Void> func = (Integer yf, Integer xf) -> {
                if (world[yf][xf].type == 1 - world[newAgeList[ran].point.y][newAgeList[ran].point.x].type) {
                    counterRef.set(counterRef.get() + 1);
                }
                return null;
            };
            Cell.goingAroundWithFunc9(func, newAgeList[ran].point.y, newAgeList[ran].point.x);
            int counter = counterRef.get();
            if (counter > worldSmoothingTreshold) {
                world[newAgeList[ran].point.y][newAgeList[ran].point.x].type = 1 - world[newAgeList[ran].point.y][newAgeList[ran].point.x].type;

                BiFunction<Cell, Cell, Boolean> check = (Cell cell, Cell cell2) -> {
                    return true;
                };
                BiFunction<Cell, Cell, Void> make = (Cell cell, Cell cell2) -> {
                    return null;
                };

                //cl++;
                AddToAllArrays(newAgeList[ran].point.x, newAgeList[ran].point.y, dArrayOfLists, newAgeList, newAgePtr, check, make);
            } else {
                int y = newAgeList[ran].point.y, x = newAgeList[ran].point.x;
                for (int i = 0; i < dArrayOfLists[y][x].ptr; ++i) {
                    int curPtr = dArrayOfLists[y][x].arr[i];
                    --newAgePtr;
                    dArrayOfLists[newAgeList[newAgePtr].point.y][newAgeList[newAgePtr].point.x].arr[newAgeList[newAgePtr].num] = curPtr;
                    newAgeList[curPtr] = newAgeList[newAgePtr];
                }
                dArrayOfLists[y][x].ptr = 0;
            }
        }


        cwspMAX = safeSWCP;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void Generate(int sizeY, int sizeX, ArrayList<GenerationInniter> generationInniters) {

        MainActivity.Debug("\n\n\n-------------------WORLD CREATION: START-------------------\n");

        MainActivity.Debug("Creating and deleting necessary things...\n");
        DrawManager.horizontalOffset = 0;
        if (world != null) {
            // Delete all world stuff

        }
        world = new Cell[sizeY][];
        for (int y = 0; y < sizeY; ++y) {
            world[y] = new Cell[sizeX];
            for (int x = 0; x < sizeX; ++x) {
                world[y][x] = new Cell();
            }
        }
        DrawManager.updateAll = true;

        MainActivity.Debug("Creating super big arrays...\n");
        CoolWSPointer[][] dArrayOfList = new CoolWSPointer[sizeY][];
        PointNNumInCWSP[] newAgeList = new PointNNumInCWSP[sizeX * sizeY * 8];
        for (int i = 0; i < newAgeList.length; ++i) {
            newAgeList[i] = new PointNNumInCWSP();
        }
        Integer newAgePtr = 0;
        for (int y = 0; y < sizeY; ++y) {
            dArrayOfList[y] = new CoolWSPointer[sizeX];
            for (int j = 0; j < sizeX; ++j) {
                dArrayOfList[y][j] = new CoolWSPointer();
            }
        }

        int counter = 0;
        for (GenerationInniter inniter : generationInniters) {
            ArrayList<Point> seeds = new ArrayList<Point>();
            MainActivity.Debug("Placing seeds #" + counter++ + "...\n");
            PlaceSeeds(seeds, inniter);
            GrowSeeds(seeds, inniter, dArrayOfList, newAgePtr, newAgeList, sizeY, sizeX);
        }
        Smoothing(dArrayOfList, newAgePtr, newAgeList, sizeY, sizeX);
    }

    //Inner data classes:
    private static class PointNNumInCWSP {
        public Point point;
        public Cell whoAdded = null;
        public int num;
    }

    private static class CoolWSPointer {
        public int ptr = 0;
        int[] arr;

        public CoolWSPointer() {
            arr = new int[8];
        }
    }

    public static class GenerationInniter {
        public int minSeeds, maxSeeds;
        public float minProc, maxProc;
        public int typeToTransformInto, typeToTransformFrom;

        GenerationInniter(int minSeeds, int maxSeeds, float minProc, float maxProc, int typeToTransformFrom, int typeToTransformInto) {
            this.minSeeds = minSeeds;
            this.maxSeeds = maxSeeds;
            this.minProc = minProc;
            this.maxProc = maxProc;
            this.typeToTransformInto = typeToTransformInto;
            this.typeToTransformFrom = typeToTransformFrom;
        }
    }


}
