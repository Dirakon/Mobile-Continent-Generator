package com.grenterinc.continenttest;

import android.os.Build;
import android.util.MutableInt;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import androidx.annotation.RequiresApi;


public class Generator {

    private static final int worldSmoothingTreshold = 4;//1...7, but recommended to leave at 4.
    public static int moreDiv = 1; //How much seas are more rare than land regions. The more number is, less sea regions there are.
    private static int cwspMAX = 5; //1...8, really takes much memory, but (probably) the effect is worth it? Can be around 3 if there are memory problems.

    @RequiresApi(api = Build.VERSION_CODES.N)
    static private void AddToAllArrays(int x, int y, MutableInt ptr,
                                       BiFunction<Integer, Integer, Boolean> checkFunc,
                                       BiFunction<Integer, Integer, Void> doFunc) {
        AddToAllArrays(x, y, ptr, checkFunc, doFunc, -1);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    static private void AddToAllArrays(int x, int y, MutableInt ptr,
                                       BiFunction<Integer, Integer, Boolean> checkFunc,
                                       BiFunction<Integer, Integer, Void> doFunc,
                                       int whoAddedThisId) {


        //Clear all pointer to this block.
        int fPtrId = GenerationBufferData.getPTRid(y, x);
        int fPtr = GenerationBufferData.getPtr(fPtrId);
        int fArrId = GenerationBufferData.getArrId(fPtrId);
        for (int i = fArrId; i < fPtr; ++i) {
            int curPtr = GenerationBufferData.getArr(i);
            ptr.value -= 1;
            int pointY = NewAgeList.getYs(ptr.value);
            int pointX = NewAgeList.getXs(ptr.value);
            int num = NewAgeList.getNum(ptr.value);
            int localArrId = GenerationBufferData.getArrId(pointY, pointX) + num;
            GenerationBufferData.setArr(localArrId, curPtr);
            NewAgeList.setFirstAsSecond(curPtr, ptr.value);
            //  newAgeList[curPtr] = newAgeList[ptr];
        }
        GenerationBufferData.setPtr(fPtrId, 0);

        //We make this block WHATEVER TYPE WE ASKED FOR
        final int id = Cell.getIdByCoords(y, x);
        doFunc.apply(id, whoAddedThisId);

        //Atomic copy of ptr

        AtomicReference<MutableInt> timelyPtr = new AtomicReference<MutableInt>(ptr);

        //We add pointer to new NON-(THIS TYPE) neighbours

        BiFunction<Integer, Integer, Void> func = (Integer yf, Integer xf) -> {
            int idf = Cell.getIdByCoords(yf, xf);
            MutableInt biFuncNewAgePtrRef = timelyPtr.get();
            int biFuncNewAgePtr = biFuncNewAgePtrRef.value;
            int biFuncGenerationBufferPtrId = GenerationBufferData.getPTRid(yf, xf);
            int biFuncGenBufREALPTR = GenerationBufferData.getPtr(biFuncGenerationBufferPtrId);
            if (checkFunc.apply(idf, id) && biFuncGenBufREALPTR != cwspMAX) {
                NewAgeList.setXs(biFuncNewAgePtr, xf);
                NewAgeList.setYs(biFuncNewAgePtr, yf);
                NewAgeList.setNum(biFuncNewAgePtr, biFuncGenBufREALPTR);
                NewAgeList.setWhoAdded(biFuncNewAgePtr, id);
                int realArrId = GenerationBufferData.getArrId(biFuncGenerationBufferPtrId) + biFuncGenBufREALPTR;
                GenerationBufferData.setArr(realArrId, biFuncNewAgePtr);
                GenerationBufferData.setPtr(biFuncGenerationBufferPtrId, biFuncGenBufREALPTR + 1); //Increment of current genBufData entry
                biFuncNewAgePtrRef.value += 1;
                timelyPtr.set(biFuncNewAgePtrRef); //Increment of NewAgePtr
            }
            return null;
        };
        Cell.goingAroundWithFunc9(func, y, x);

        //Reset ptr
        ptr = timelyPtr.get();
    }

    static private void clearPTRs(MutableInt newAgePtr, int sizeY, int sizeX) {
        newAgePtr.value = 0;
        int totalSize = sizeX * sizeY;
        for (int id = 0; id < totalSize; ++id) {
            GenerationBufferData.setPtr(id, 0);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    static private void GrowSeeds(ArrayList<Point> seedsList, GenerationInniter inniter, MutableInt newAgePtr, int sizeY, int sizeX) {
        BiFunction<Integer, Integer, Boolean> boolIfOther = (Integer cell, Integer whoAdded) -> {
            return Cell.getTypeOfCell(cell) == inniter.typeToTransformFrom;
        };
        BiFunction<Integer, Integer, Void> makeThis = (Integer cell, Integer whoAdded) -> {
            Cell.setTypeOfCell(cell, inniter.typeToTransformInto);
            return null;
        };
        for (Point seed : seedsList) {
            clearPTRs(newAgePtr, sizeY, sizeX);
            int landAmount = (int) (Std.inBetweenTwoFloats(inniter.minProc, inniter.maxProc) * (sizeX * sizeY));
            int orx = seed.x;
            int ory = seed.y;
            int idf = Cell.getIdByCoords(ory, orx);
            Cell.setTypeOfCell(idf, inniter.typeToTransformFrom);
            AddToAllArrays(orx, ory, newAgePtr, boolIfOther, makeThis);

            for (int curLand = 0; curLand < landAmount && newAgePtr.value != 0; ++curLand) {
                int pol = new Random().nextInt(newAgePtr.value);
                AddToAllArrays(NewAgeList.getXs(pol), NewAgeList.getYs(pol), newAgePtr, boolIfOther, makeThis);

            }
        }
    }

    static private void PlaceSeeds(ArrayList<Point> seedsList, GenerationInniter inniter) {
        //  int cura = pointsList.size();
        int continentSeeds = Std.inBetweenTwoInts(inniter.minSeeds, inniter.maxSeeds);
        for (int i = 0; i < continentSeeds; ++i) {
            int y, x;
            int ider;
            do {
                y = new Random().nextInt(Cell.sizeY);
                x = new Random().nextInt(Cell.sizeX);
                ider = Cell.getIdByCoords(y, x);
            } while (Cell.getTypeOfCell(ider) == inniter.typeToTransformInto);
            Cell.setTypeOfCell(ider, inniter.typeToTransformInto);
            Point p = new Point();
            p.y = y;
            p.x = x;
            seedsList.add(p);
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private static void Smoothing(MutableInt newAgePtr, int sizeY, int sizeX) {
        int safeSWCP = cwspMAX;
        cwspMAX = 8;

        clearPTRs(newAgePtr, sizeY, sizeX);
        MainActivity.Debug("Smoothing mapping...\n");
        for (int y = 0; y < sizeY; ++y) {
            for (int x = 0; x < sizeX; ++x) {
                AtomicReference<Integer> counterRef = new AtomicReference<Integer>(0);
                int finalID = Cell.getIdByCoords(y, x);
                BiFunction<Integer, Integer, Void> func = (Integer yf, Integer xf) -> {
                    int ider = Cell.getIdByCoords(yf, xf);
                    if (Cell.getTypeOfCell(ider) != Cell.getTypeOfCell(finalID)) {
                        counterRef.set(counterRef.get() + 1);
                    }
                    return null;
                };
                Cell.goingAroundWithFunc9(func, y, x);
                int counter = counterRef.get();
                int genBufDataPtrId = GenerationBufferData.getPTRid(y, x);
                int genBufDataREALPTR = GenerationBufferData.getPtr(genBufDataPtrId);
                if (counter > 0 && genBufDataREALPTR != cwspMAX) {
                    NewAgeList.setYs(newAgePtr.value, y);
                    NewAgeList.setNum(newAgePtr.value, genBufDataREALPTR);
                    NewAgeList.setXs(newAgePtr.value, x);
                    int actArrId = GenerationBufferData.getArrId(genBufDataPtrId) + genBufDataREALPTR;
                    GenerationBufferData.setArr(actArrId, newAgePtr.value);
                    newAgePtr.value += 1;
                    GenerationBufferData.setPtr(genBufDataPtrId, genBufDataREALPTR + 1);
                }
            }
        }
        MainActivity.Debug("Actual smoothing...\n");
        while (newAgePtr.value != 0) {
            int ran = new Random().nextInt(newAgePtr.value);
            int ranId = Cell.getIdByCoords(NewAgeList.getYs(ran), NewAgeList.getXs(ran));
            AtomicReference<Integer> counterRef = new AtomicReference<Integer>(0);

            BiFunction<Integer, Integer, Void> func = (Integer yf, Integer xf) -> {
                int id = Cell.getIdByCoords(yf, xf);
                if (Cell.getTypeOfCell(id) == 1 - Cell.getTypeOfCell(ranId)) {
                    counterRef.set(counterRef.get() + 1);
                }
                return null;
            };
            Cell.goingAroundWithFunc9(func, NewAgeList.getYs(ran), NewAgeList.getXs(ran));// newAgeList[ran].point.y, newAgeList[ran].point.x);
            int counter = counterRef.get();
            if (counter > worldSmoothingTreshold) {
                Cell.setTypeOfCell(ranId, 1 - Cell.getTypeOfCell(ranId));

                BiFunction<Integer, Integer, Boolean> check = (Integer cell, Integer cell2) -> {
                    return true;
                };
                BiFunction<Integer, Integer, Void> make = (Integer cell, Integer cell2) -> {
                    return null;
                };

                //cl++;
                AddToAllArrays(NewAgeList.getXs(ran), NewAgeList.getYs(ran), newAgePtr, check, make);
            } else {
                int y = NewAgeList.getYs(ran), x = NewAgeList.getXs(ran);
                int curGenBufPtrId = GenerationBufferData.getPTRid(y, x);
                int curGenBufREALPTR = GenerationBufferData.getPtr(curGenBufPtrId);
                int curGenBufArrPTR = GenerationBufferData.getArrId(curGenBufPtrId);
                for (int i = 0; i < curGenBufREALPTR; ++i) {
                    int curPtr = GenerationBufferData.getArr(curGenBufArrPTR + curGenBufREALPTR);//dArrayOfLists[y][x].arr[i];
                    --newAgePtr.value;
                    int yr = NewAgeList.getYs(newAgePtr.value), xr = NewAgeList.getXs(newAgePtr.value), num = NewAgeList.getNum(newAgePtr.value);
                    int newArrId = GenerationBufferData.getArrId(yr, xr) + num;
                    GenerationBufferData.setArr(newArrId, curPtr);
                    NewAgeList.setFirstAsSecond(curPtr, newAgePtr.value);
                }
                GenerationBufferData.setPtr(curGenBufPtrId, 0);
            }
        }


        cwspMAX = safeSWCP;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void Generate(int sizeY, int sizeX, ArrayList<GenerationInniter> generationInniters) {

        MainActivity.Debug("\n\n\n-------------------WORLD CREATION: START-------------------\n");

        MainActivity.Debug("Creating and deleting necessary things...\n");
        DrawManager.horizontalOffset = 0;
        Cell.initWorld(sizeY, sizeX);
        DrawManager.updateAll = true;

        MainActivity.Debug("Creating super big arrays...\n");
        // CoolWSPointer[][] dArrayOfList = new CoolWSPointer[sizeY][];
        //PointNNumInCWSP[] newAgeList = new PointNNumInCWSP[sizeX * sizeY * 8];
        NewAgeList.initNewAgeList(sizeX * sizeY * 8);
        MutableInt newAgePtr = new MutableInt(0);
        GenerationBufferData.initGenerationBufferData(sizeY, sizeX);

        int counter = 0;
        for (GenerationInniter inniter : generationInniters) {
            ArrayList<Point> seeds = new ArrayList<Point>();
            MainActivity.Debug("Placing seeds #" + counter + "...\n");
            PlaceSeeds(seeds, inniter);
            MainActivity.Debug("Growing seeds #" + counter++ + "...\n");
            GrowSeeds(seeds, inniter, newAgePtr, sizeY, sizeX);
        }
        Smoothing(newAgePtr, sizeY, sizeX);
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

class NewAgeList { //TURN INTO SINGLE ARRAY
    //private static Point point[];
    private static int[] xs;
    private static int[] ys;
    private static int[] whoAdded;// = -1;
    private static int[] num;

    private NewAgeList() {

    }

    public static void setFirstAsSecond(int id1, int id2) {
        xs[id1] = xs[id2];
        ys[id1] = ys[id2];
        whoAdded[id1] = whoAdded[id2];
        num[id1] = num[id2];
    }

    public static void initNewAgeList(int totalSize) {
        xs = new int[totalSize];
        ys = new int[totalSize];
        num = new int[totalSize];
        whoAdded = new int[totalSize];
        for (int i = 0; i < totalSize; ++i) {
            whoAdded[i] = -1;
        }
    }

    public static int getXs(int id) {
        return xs[id];
    }

    public static void setXs(int id, int data) {
        xs[id] = data;
    }

    public static int getYs(int id) {
        return ys[id];
    }

    public static void setYs(int id, int data) {
        ys[id] = data;
    }

    public static int getWhoAdded(int id) {
        return whoAdded[id];
    }

    public static void setWhoAdded(int id, int data) {
        whoAdded[id] = data;
    }

    public static int getNum(int id) {
        return num[id];
    }

    public static void setNum(int id, int data) {
        num[id] = data;
    }
}

class GenerationBufferData {    //TURN INTO DOUBLE ARRAY
    private static int[] ptr;// = 0
    private static int[] arr;//arr = new int[8];

    private GenerationBufferData() {

    }

    public static void initGenerationBufferData(int y, int x) {
        int totalSize = y * x;
        ptr = new int[totalSize];
        arr = new int[totalSize * 8];
    }

    public static int getPTRid(int y, int x) {
        return y * Cell.sizeX + x;
    }

    public static int getArrId(int y, int x) {
        return getPTRid(y, x) * 8;
    }

    public static int getArrId(int id) {
        return id * 8;
    }

    public static int getPtr(int id) {
        return ptr[id];
    }

    public static void setPtr(int id, int data) {
        ptr[id] = data;
    }

    public static int getArr(int id) {
        return arr[id];
    }

    public static void setArr(int id, int data) {
        arr[id] = data;
    }
}