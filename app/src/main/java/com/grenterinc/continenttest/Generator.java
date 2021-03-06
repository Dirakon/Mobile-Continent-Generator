package com.grenterinc.continenttest;

import android.os.Build;
import android.util.MutableInt;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Random;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import androidx.annotation.RequiresApi;

import static com.grenterinc.continenttest.Cell.LAND;
import static com.grenterinc.continenttest.Cell.WATER;
import static com.grenterinc.continenttest.Cell.goingAroundWithFunc9;
import static com.grenterinc.continenttest.Region.regions;
import static com.grenterinc.continenttest.Std.inBetweenTwoFloats;
import static com.grenterinc.continenttest.Std.inBetweenTwoInts;
import static com.grenterinc.continenttest.TerrainType.PLANE;
import static com.grenterinc.continenttest.TerrainType.getTerrainTypeByDeepness;
import static com.grenterinc.continenttest.TerrainType.terrainTypes;
import static java.lang.Math.sqrt;
import static java.lang.StrictMath.abs;


public class Generator {

    private static final int worldSmoothingTreshold = 4;//1...7, but recommended to leave at 4.
    public static int moreDiv = 1; //How much seas are more rare than land regions. The more number is, less sea regions there are.
    private static int cwspMAX = 8; //1...8, really takes much memory, but (probably) the effect is worth it? Can be around 3 if there are memory problems.
    private static final float amountOfRegionsProcent = 0.03f;//How much regions. The more number is, less regions there are.
    private static final int terrainTypeSmoothingCycles = 3;
    private static final int regionSmoothingCycles = 3; //doesn't really matter that much, better leave at 3, but not less.
    private static final int regionSmoothingTreshold = 4; //1...7, but recommended to leave at 4.
    private static final int chanceForLandToBeRiverSpawn = 10;
    private static final int minRiverSize = 4, maxRiverSize = 18; //
    private static final float minWaterMassPerDeformation = 0.0002f, maxWaterMassPerDeformation = 0.015f;
    private static final float chanceForDeformationToAppear = 0.002f;

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
        for (int i = fArrId; i < fPtr + fArrId; ++i) {
            int curPtr = GenerationBufferData.getArr(i);
            ptr.value -= 1;
            int pointY = NewAgeList.getYs(ptr.value);
            int pointX = NewAgeList.getXs(ptr.value);
            int num = NewAgeList.getNum(ptr.value);
            int localArrId = GenerationBufferData.getArrId(pointY, pointX) + num;
            GenerationBufferData.setArr(localArrId, curPtr);
            NewAgeList.setFirstAsSecond(curPtr, ptr.value);
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
    static private void GrowSeeds(LinkedList<Point> seedsList, GenerationInniter inniter, MutableInt newAgePtr, int sizeY, int sizeX) {


        BiFunction<Integer, Integer, Boolean> boolIfOther = (Integer cell, Integer whoAdded) -> {
            return Cell.getTypeOfCell(cell) == inniter.typeToTransformFrom;
        };
        BiFunction<Integer, Integer, Void> makeThis = (Integer cell, Integer whoAdded) -> {
            Cell.setTypeOfCell(cell, inniter.typeToTransformInto);
            return null;
        };
        for (Point seed : seedsList) {
            clearPTRs(newAgePtr, sizeY, sizeX);

            // Get amount of cells that this 'continent' (or whatever) will take
            int landAmount = (int) (inBetweenTwoFloats(inniter.minProc, inniter.maxProc) * (sizeX * sizeY));
            int orx = seed.x;
            int ory = seed.y;
            int idf = Cell.getIdByCoords(ory, orx);

            //Place our starting cell's neighbours into our list
            Cell.setTypeOfCell(idf, inniter.typeToTransformFrom);
            AddToAllArrays(orx, ory, newAgePtr, boolIfOther, makeThis);

            // And start the regular iteration until either we have no room or we did enough
            for (int curLand = 0; curLand < landAmount && newAgePtr.value != 0; ++curLand) {
                int pol = new Random().nextInt(newAgePtr.value);
                AddToAllArrays(NewAgeList.getXs(pol), NewAgeList.getYs(pol), newAgePtr, boolIfOther, makeThis);

            }

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    static private void PlaceSeeds(LinkedList<Point> seedsList, GenerationInniter inniter) {

        int continentSeeds = inBetweenTwoInts(inniter.minSeeds, inniter.maxSeeds);

        //Inserts <continentSeeds> random points that fit the description in <inniter> into <seedsList>

        // WARNING! Will be an infinite loops if there are no cells (or few) that fit the description
        if (inniter.isDeformation) {
            //LAND DEFORMATION UP AHEAD!
            for (int y = 0; y < Cell.sizeY; ++y) {
                for (int x = 0; x < Cell.sizeX; ++x) {
                    if (Std.inBetweenTwoFloats(0, 1) < chanceForDeformationToAppear) {
                        int id = Cell.getIdByCoords(y, x);
                        if (Cell.getTypeOfCell(id) == LAND) {
                            AtomicReference<Integer> counterRef = new AtomicReference<Integer>(0);

                            //Counter of cells with different types.
                            BiFunction<Integer, Integer, Void> func = (Integer yf, Integer xf) -> {
                                int ider = Cell.getIdByCoords(yf, xf);
                                if (Cell.getTypeOfCell(ider) != LAND) {
                                    counterRef.set(counterRef.get() + 1);
                                }
                                return null;
                            };

                            // Actually count different cells
                            Cell.goingAroundWithFunc9(func, y, x);

                            // If there are different cells, we add this cell to seed list
                            if (counterRef.get() > 0) {
                                seedsList.add(new Point(x, y));
                            }
                        }
                    }
                }
            }


        } else {
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
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private static void Smoothing(MutableInt newAgePtr, int sizeY, int sizeX) {

        //We need all 8 neighbouring cells around to be checked in order for smoothing to be effective

        int safeSWCP = cwspMAX;
        cwspMAX = 8;

        clearPTRs(newAgePtr, sizeY, sizeX);


        for (int y = 0; y < sizeY; ++y) {
            for (int x = 0; x < sizeX; ++x) {

                // Counter of neighbouring cells that differ from our cell
                AtomicReference<Integer> counterRef = new AtomicReference<Integer>(0);
                int finalID = Cell.getIdByCoords(y, x);
                BiFunction<Integer, Integer, Void> func = (Integer yf, Integer xf) -> {
                    int ider = Cell.getIdByCoords(yf, xf);
                    if (Cell.getTypeOfCell(ider) != Cell.getTypeOfCell(finalID)) {
                        counterRef.set(counterRef.get() + 1);
                    }
                    return null;
                };

                // Actually count different cells
                Cell.goingAroundWithFunc9(func, y, x);

                int counter = counterRef.get();
                int genBufDataPtrId = GenerationBufferData.getPTRid(y, x);
                int genBufDataREALPTR = GenerationBufferData.getPtr(genBufDataPtrId);

                // If there are different cells, we add this cell to our list, for future smoothing
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

        while (newAgePtr.value != 0) {

            //Pick a random cell
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

            //Count all the different cells
            Cell.goingAroundWithFunc9(func, NewAgeList.getYs(ran), NewAgeList.getXs(ran));
            int counter = counterRef.get();

            //If there are enough do be smoothed,
            if (counter > worldSmoothingTreshold) {

                //Change the type

                Cell.setTypeOfCell(ranId, 1 - Cell.getTypeOfCell(ranId));

                BiFunction<Integer, Integer, Boolean> check = (Integer cell, Integer cell2) -> {
                    return true;
                };
                BiFunction<Integer, Integer, Void> make = (Integer cell, Integer cell2) -> {
                    return null;
                };

                // Add all neighbours that are under suspicion into our list

                AddToAllArrays(NewAgeList.getXs(ran), NewAgeList.getYs(ran), newAgePtr, check, make);
            } else {

                // Else delete this cell from list and from all the neighbours' lists

                int y = NewAgeList.getYs(ran), x = NewAgeList.getXs(ran);
                int curGenBufPtrId = GenerationBufferData.getPTRid(y, x);
                int curGenBufREALPTR = GenerationBufferData.getPtr(curGenBufPtrId);
                int curGenBufArrPTR = GenerationBufferData.getArrId(curGenBufPtrId);
                for (int i = 0; i < curGenBufREALPTR; ++i) {
                    int curPtr = GenerationBufferData.getArr(curGenBufArrPTR + i);
                    --newAgePtr.value;
                    int yr = NewAgeList.getYs(newAgePtr.value), xr = NewAgeList.getXs(newAgePtr.value), num = NewAgeList.getNum(newAgePtr.value);
                    int newArrId = GenerationBufferData.getArrId(yr, xr) + num;
                    GenerationBufferData.setArr(newArrId, curPtr);
                    NewAgeList.setFirstAsSecond(curPtr, newAgePtr.value);
                }
                GenerationBufferData.setPtr(curGenBufPtrId, 0);
            }
        }


        // Return this 'magical' value to it's assigned state
        cwspMAX = safeSWCP;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    static private void PlaceRegionSeeds(int sizeY, int sizeX, MutableInt newAgePtr, LinkedList<Point> regionSeeds) {
        int toSkip = (int) (sqrt(sizeX * sizeY) * amountOfRegionsProcent);
        for (int y = toSkip; y < sizeY; y += toSkip) {
            for (int x = toSkip; x < sizeX; x += toSkip) {
                int yxId = Cell.getIdByCoords(y, x);
                if (Cell.getTypeOfCell(yxId) == WATER) {
                    if ((x / toSkip) % moreDiv != 0 || (y / toSkip) % moreDiv != 0) {
                        continue;
                    }
                }
                DrawManager.cellsForUpdate.push(new Point(x, y));
                Cell.setRegionOfCell(yxId, regionSeeds.size());
                regionSeeds.add(new Point(x, y));
                BiFunction<Integer, Integer, Boolean> firstCheck = (Integer thisCell, Integer whoAdded) -> {
                    return whoAdded == -1 || (Cell.getTypeOfCell(whoAdded) == Cell.getTypeOfCell(thisCell) && Cell.getRegionOfCell(thisCell) == -1);
                };
                BiFunction<Integer, Integer, Void> firstMake = (Integer thisCell, Integer whoAdded) -> {
                    return null;
                };
                AddToAllArrays(x, y, newAgePtr, firstCheck, firstMake);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    static private void GrowingRegionSeeds(MutableInt newAgePtr, LinkedList<Point> regionSeeds) {
        BiFunction<Integer, Integer, Boolean> checkFunc = (Integer thisCell, Integer whoAdded) -> {
            return whoAdded == -1 || (Cell.getTypeOfCell(whoAdded) == Cell.getTypeOfCell(thisCell) && Cell.getRegionOfCell(thisCell) == -1);
        };
        BiFunction<Integer, Integer, Void> makeFunc = (Integer thisCell, Integer whoAdded) -> {
            Cell.setRegionOfCell(thisCell, Cell.getRegionOfCell(whoAdded));
            return null;
        };
        while (newAgePtr.value != 0) {
            int pol = new Random().nextInt(newAgePtr.value);

            AddToAllArrays(NewAgeList.getXs(pol), NewAgeList.getYs(pol), newAgePtr, checkFunc, makeFunc, NewAgeList.getWhoAdded(pol));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    static private void AddMissingRegions(int sizeY, int sizeX, LinkedList<Point> regionSeeds) {
        for (int y = 0; y < sizeY; ++y) {
            for (int x = 0; x < sizeX; ++x) {
                int yxId = Cell.getIdByCoords(y, x);
                if (Cell.getRegionOfCell(yxId) == -1) {
                    Stack<Point> ava = new Stack<Point>();
                    ava.push(new Point(x, y));
                    int reg = regionSeeds.size();
                    regionSeeds.add(ava.peek());
                    while (!ava.empty()) {
                        Point p = ava.pop();
                        int pointId = Cell.getIdByCoords(p.y, p.x);
                        Cell.setRegionOfCell(pointId, reg);
                        BiFunction<Integer, Integer, Void> func = (Integer yf, Integer xf) -> {
                            int yfxfId = Cell.getIdByCoords(yf, xf);
                            if (Cell.getRegionOfCell(yfxfId) == -1 && Cell.getTypeOfCell(yfxfId) == Cell.getTypeOfCell(pointId)) {
                                ava.push(new Point(xf, yf));
                                Cell.setRegionOfCell(yfxfId, reg);
                            }
                            return null;
                        };
                        Cell.goingAroundWithFunc9(func, p.y, p.x);
                    }
                }
            }
        }
        DrawManager.updateAll = true;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    static private void SmoothRegions(int sizeY, int sizeX) {
        for (int i = 0; i < regionSmoothingCycles; ++i) {
            MainActivity.Debug("Smoothing cycle #" + (i + 1) + "...\n");
            for (int y = 0; y < sizeY; ++y) {
                for (int x = 0; x < sizeX; ++x) {
                    final AtomicInteger[] counterDifType = {new AtomicInteger()};
                    final AtomicInteger[] counterOurCountry = {new AtomicInteger()};
                    final int[] bestCountry = {-1};
                    final int[] ptr = {0};
                    int[] otherCountries = new int[8], theirAmounts = new int[8];
                    int yxId = Cell.getIdByCoords(y, x);
                    BiFunction<Integer, Integer, Void> func = (Integer yf, Integer xf) -> {
                        int yfxfId = Cell.getIdByCoords(yf, xf);
                        if (Cell.getTypeOfCell(yfxfId) == 1 - Cell.getTypeOfCell(yxId)) {
                            counterDifType[0].getAndIncrement();
                        } else if (Cell.getRegionOfCell(yfxfId) == Cell.getRegionOfCell(yxId)) {
                            counterOurCountry[0].getAndIncrement();
                        } else {
                            boolean weInIt = false;
                            for (int jj = 0; jj < ptr[0]; ++jj) {
                                if (otherCountries[jj] == Cell.getRegionOfCell(yfxfId)) {
                                    weInIt = true;
                                    theirAmounts[jj]++;
                                    if (bestCountry[0] == -1 || theirAmounts[jj] > theirAmounts[bestCountry[0]]) {
                                        bestCountry[0] = jj;
                                    }
                                    break;
                                }
                            }
                            if (!weInIt) {
                                otherCountries[ptr[0]] = Cell.getRegionOfCell(yfxfId);
                                if (bestCountry[0] == -1) {
                                    bestCountry[0] = ptr[0];

                                }
                                theirAmounts[ptr[0]++] = 1;
                            }
                        }
                        return null;
                    };
                    goingAroundWithFunc9(func, y, x);
                    if (bestCountry[0] != -1 && counterOurCountry[0].get() - 1 < regionSmoothingTreshold - counterDifType[0].get()) {
                        Cell.setRegionOfCell(yxId, otherCountries[bestCountry[0]]);
                    }
                }
            }
        }    //ourBlocks < 4 - water
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    static private void CreateRivers(int sizeY, int sizeX) {
        LinkedList<BorderWithRegion> riverSeeds = new LinkedList<BorderWithRegion>();
        int riverPTR = 0;

        for (int i = 0; i < Region.landRegions.length; ++i) {
            if (getTerrainTypeByDeepness(Region.landRegions[i].deepness).canRiverSpawn && (new Random().nextInt(100) < chanceForLandToBeRiverSpawn)) {
                for (BorderWithRegion bord : Region.landRegions[i].borders) {

                    if (regions[bord.neighbourId].type == LAND && getTerrainTypeByDeepness(regions[bord.neighbourId].deepness).canRiverSpawn) {
                        riverSeeds.add(bord);
                        break;
                    }
                }
            }
        }

        for (BorderWithRegion riv : riverSeeds) {
            int size = inBetweenTwoInts(minRiverSize, maxRiverSize) - 1;
            BorderWithRegion next = BorderWithRegion.makeBorderRiver(riv);
            while (next != null && size > 0) {
                size--;
                next = BorderWithRegion.makeBorderRiver(next);
            }
        }

    }

    static private void SmoothTheTerrain(int sizeY, int sizeX) {
        for (int cyc = 0; cyc < terrainTypeSmoothingCycles; ++cyc) {
            for (int i = 0; i < Region.landRegions.length; ++i) {
                TerrainType terType = getTerrainTypeByDeepness(Region.landRegions[i].deepness);
                if (!terType.canBeSmoothed)
                    continue;
                int[] types = new int[terrainTypes.length];
                int max = 0;
                int tprt = -1;
                int allBorders = Region.landRegions[i].borders.size();
                for (BorderWithRegion bord : Region.landRegions[i].borders) {
                    Region nei = regions[bord.neighbourId];
                    if (nei.type != LAND) {
                        allBorders--;
                    } else {
                        int cur = -nei.deepness - 1;
                        if (++types[cur] > max && (terrainTypes[cur].canSmoothOthers || cur == -Region.landRegions[i].deepness - 1)) {
                            tprt = cur;
                            max = types[cur];
                        }
                    }
                }
                if (tprt == -1)
                    continue;
                if (types[-Region.landRegions[i].deepness - 1] / (float) allBorders < terType.smoothingTreshold) {
                    Region.landRegions[i].deepness = -tprt - 1;
                }
            }
        }
    }

    static private void PlaceTerrainTypes(int sizeY, int sizeX) {
        float halfy = sizeY / 2;
        for (int i = 0; i < Region.landRegions.length; ++i) {
            float crioDist = inBetweenTwoFloats(terrainTypes[1].minMinEquatorDistance, terrainTypes[1].maxMinEquatorDistance);
            float rastFromEquator = abs((Region.landRegions[i].center.y - halfy) / halfy);
            if (rastFromEquator >= crioDist) {
                Region.landRegions[i].deepness = TerrainType.SNOW;
            } else {
                if (Region.landRegions[i].deepness == PLANE) {
                    boolean cycleEnd = false;
                    int type = 0;
                    int amount = 0;
                    for (int curType = TerrainType.SPAWNABLETYPES; curType < terrainTypes.length; ++curType) {
                        float minEquat = inBetweenTwoFloats(terrainTypes[curType].minMinEquatorDistance, terrainTypes[curType].maxMinEquatorDistance);
                        float maxEquat = inBetweenTwoFloats(terrainTypes[curType].minMaxEquatorDistance, terrainTypes[curType].maxMaxEquatorDistance);
                        amount = inBetweenTwoInts(terrainTypes[curType].minSize, terrainTypes[curType].maxSize);
                        if (rastFromEquator >= minEquat && rastFromEquator <= maxEquat && new Random().nextInt(100) < terrainTypes[curType].chanceToSpawn) {
                            type = -curType - 1;
                            break;
                        }
                    }
                    if (type == 0)
                        continue;

                    Region.landRegions[i].deepness = type;
                    final int chance = 10;
                    LinkedList<Region> regs = new LinkedList<Region>();
                    for (BorderWithRegion r : Region.landRegions[i].borders) {

                        Region reg = regions[r.neighbourId];
                        if (reg.deepness == PLANE) {
                            regs.add(reg);
                        }
                    }
                    if (regs.size() != 0) {
                        while (true) {
                            LinkedList<Region> regionsForFuture = new LinkedList<>();
                            for (ListIterator<Region> it = regs.listIterator(); it.hasNext(); ) {
                                boolean doIt = true;
                                if (new Random().nextInt(100) < chance) {
                                    Region actualThing = it.next();
                                    it.previous();
                                    actualThing.deepness = type;
                                    for (BorderWithRegion r : actualThing.borders) {
                                        Region reg = regions[r.neighbourId];
                                        if (reg.deepness == PLANE) {
                                            regionsForFuture.add(reg);
                                        }
                                    }
                                    Iterator<Region> saveIt = it;
                                    if (!it.hasPrevious()) {
                                        doIt = false;
                                        if (it.hasNext())
                                            it.next();
                                    } else {
                                        it.previous();
                                    }
                                    saveIt.remove();
                                    amount--;
                                    if (regs.size() == 0 || amount <= 0) {
                                        cycleEnd = true;
                                        break;
                                    }

                                }
                                if (doIt) {
                                    it.next();
                                }
                            }
                            if (cycleEnd)
                                break;
                            regs.addAll(regionsForFuture);
                        }
                    }
                    if (cycleEnd)
                        continue;
                }

            }
        }
    }

    static private void CalculateSeaDeepness(int sizeY, int sizeX) {
        boolean thereSmthToDo;
        do {
            thereSmthToDo = false;
            for (Region region : regions) {
                if (region.type == WATER) {
                    int max = region.deepness;
                    for (BorderWithRegion borderWithRegion : region.borders) {
                        int realValue = regions[borderWithRegion.neighbourId].deepness;
                        if (realValue != -2 && (realValue + 1 < max || max == -2)) {
                            max = realValue + 1;
                            thereSmthToDo = true;
                        }
                    }
                    region.deepness = max;
                }
            }
        } while (thereSmthToDo);
    }

    static private void AssignRandomColorsToRegions(int sizeY, int sizeX) {
        for (Region region : regions) {
            region.center = new Point((region.l.x + region.r.x) / 2, (region.u.y + region.d.y) / 2);


            region.l.x %= sizeX;
            region.r.x %= sizeX;
            region.center.x %= sizeX;


            region.colorR = inBetweenTwoFloats(0, 1);
            region.colorG = inBetweenTwoFloats(0, 1);
            region.colorB = inBetweenTwoFloats(0, 1);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    static private void CreateBasicRegionData(int sizeY, int sizeX, LinkedList<Point> regionSeeds) {
        int regionAmount = regionSeeds.size();
        regions = new Region[regionAmount];
        for (int i = 0; i < regionAmount; ++i)
            regions[i] = new Region();

        for (int y = 0; y < sizeY; ++y) {
            for (int x = 0; x < sizeX; ++x) {
                int yxId = Cell.getIdByCoords(y, x);
                int regionId = Cell.getRegionOfCell(yxId);
                if (regions[regionId].d == null) {
                    regions[regionId].d = regions[regionId].l = regions[regionId].u = regions[regionId].r = new Point(x, y);
                } else {
                    if (y < regions[regionId].d.y) {
                        regions[regionId].d = new Point(x, y);
                    }
                    if (y > regions[regionId].u.y) {
                        regions[regionId].u = new Point(x, y);
                    }
                    if (regions[regionId].r.x > 0) {
                        if (x < regions[regionId].l.x) {
                            regions[regionId].l = new Point(x, y);
                        }
                        if (x > regions[regionId].r.x) {
                            regions[regionId].r = new Point(x, y);
                        }
                    }
                }
                if ((x == 0 || x == sizeX - 1) && regions[regionId].r.x >= 0) {
                    regions[regionId].l.x = Integer.MAX_VALUE;
                    regions[regionId].r.x = Integer.MIN_VALUE;
                }
            }
        }
        int landRegions = 0;

        for (int i = 0; i < regionAmount; ++i) {
            if (regions[i].r.x == -1) {
                //THIS REGION HAS NO LAND! (maybe because of smoothing, happens...)
                Region[] newRegs = new Region[regionAmount - 1];
                for (int j = 0; j < i; ++j) {
                    newRegs[j] = regions[i];
                }
                for (int j = i + 1; j < regionAmount; ++j) {
                    newRegs[j - 1] = regions[i];
                }
                regionAmount--;
                regions = newRegs;
                --i;
                continue;
            }
            int y = regions[i].u.y;
            int x = regions[i].u.x;
            int thisRegionsCellId = Cell.getIdByCoords(y, x);
            if (Cell.getTypeOfCell(thisRegionsCellId) == LAND) {
                regions[i].deepness++;
                regions[i].type = LAND;
                landRegions++;
            }
        }
        int lrPTR = 0;
        Region.landRegions = new Region[landRegions];
        boolean[] unmappableRegions = new boolean[regionAmount];
        for (int i = 0; i < regionAmount; ++i) {
            if (regions[i].type == LAND) {
                Region.landRegions[lrPTR++] = regions[i];
            }
            unmappableRegions[i] = regions[i].r.x < 0;
        }
        int half = sizeX / 2;
        for (int y = 0; y < sizeY; ++y) {
            for (int x = half; x < sizeX + half; ++x) {
                int realX = x % sizeX;
                int yRealXCellId = Cell.getIdByCoords(y, realX);
                int regId = Cell.getRegionOfCell(yRealXCellId);
                AtomicInteger counter = new AtomicInteger();
                BorderCell diso = new BorderCell(realX, y);
                BiFunction<Integer, Integer, Void> func = (Integer yf, Integer xf) ->
                {
                    int yfXfId = Cell.getIdByCoords(yf, xf);
                    if (regId != Cell.getRegionOfCell(yfXfId)) {
                        counter.getAndIncrement();
                        for (BorderWithRegion borderWithRegion : regions[regId].borders) {
                            if (borderWithRegion.neighbourId == Cell.getRegionOfCell(yfXfId)) {
                                if (borderWithRegion.cells.get(borderWithRegion.cells.size() - 1) != diso) {
                                    Cell.setBorderPartOfCell(yRealXCellId, true);
                                    borderWithRegion.cells.add(diso);
                                    counter.getAndIncrement();
                                }
                                return null; //already a border
                            }
                        }
                        BorderWithRegion bord = new BorderWithRegion();
                        bord.regionId = regId;
                        bord.neighbourId = Cell.getRegionOfCell(yfXfId);
                        Cell.setBorderPartOfCell(yRealXCellId, true);
                        bord.cells.add(diso);
                        regions[regId].borders.add(bord);
                        counter.getAndIncrement();
                    }
                    return null;
                };
                goingAroundWithFunc9(func, y, realX);
                //Point(realX, y);
                if (unmappableRegions[regId]) {
                    if (x < regions[regId].l.x) {
                        regions[regId].l = new Point(x, y);
                    }
                    if (x > regions[regId].r.x) {
                        regions[regId].r = new Point(x, y);
                    }
                }
                if (counter.get() == 0) {
                    regions[regId].cells.add(new Point(realX, y));
                }
            }
        }
    }

    static private void AnalyzeRegionsLivability(int sizeY, int sizeX) {
        for (Region reg : Region.landRegions) {
            TerrainType terrainType = reg.getTerrainType();
            reg.livability = terrainType.livability + (reg.hasRiver ? terrainType.riverLivabilityIncrease : 0);
        }
        Arrays.sort(Region.landRegions, new Comparator<Region>() {
            @Override
            public int compare(Region o1, Region o2) {
                return -Float.compare(o1.livability, o2.livability);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    static private void CreateRegionData(int sizeY, int sizeX, LinkedList<Point> regionSeeds) {

        MainActivity.Debug("Creating basic regional data...\n");
        CreateBasicRegionData(sizeY, sizeX, regionSeeds);

        MainActivity.Debug("Assigning random colors to regions...\n");
        AssignRandomColorsToRegions(sizeY, sizeX);

        MainActivity.Debug("Calculating sea deepness...\n");
        CalculateSeaDeepness(sizeY, sizeX);

        MainActivity.Debug("Placing the terrain types...\n");
        PlaceTerrainTypes(sizeY, sizeX);

        MainActivity.Debug("Smoothing the terrain types...\n");
        SmoothTheTerrain(sizeY, sizeX);

        MainActivity.Debug("Making rivers...\n");
        CreateRivers(sizeY, sizeX);

        MainActivity.Debug("Calculating the livability of regions...\n");
        AnalyzeRegionsLivability(sizeY, sizeX);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static void DivideIntoRegions(int sizeY, int sizeX, MutableInt newAgePtr) {

        MainActivity.Debug("Placing region seeds...");
        LinkedList<Point> regionSeeds = new LinkedList<Point>();
        PlaceRegionSeeds(sizeY, sizeX, newAgePtr, regionSeeds);

        MainActivity.Debug("Growing region seeds...");
        GrowingRegionSeeds(newAgePtr, regionSeeds);

        MainActivity.Debug("Adding missing regions...");
        AddMissingRegions(sizeY, sizeX, regionSeeds);

        MainActivity.Debug("Smoothing regions");
        SmoothRegions(sizeY, sizeX);

        MainActivity.Debug("Creating region data");
        CreateRegionData(sizeY, sizeX, regionSeeds);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void CreateCountries() {
        Nation.nations = new LinkedList<Nation>();
        Nation first = new Nation();
        Nation.nations.add(first);
        first.annex(Region.landRegions[0]);
        DrawManager.regionsForUpdate.add(first.regions.get(0));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void Generate(int sizeY, int sizeX, LinkedList<GenerationInniter> generationInniters) {

        MainActivity.Debug("\n\n\n-------------------WORLD CREATION: START-------------------\n");

        MainActivity.Debug("Creating and deleting necessary things...\n");

        DrawManager.horizontalOffset = 0;

        Cell.initWorld(sizeY, sizeX);

        //All cells are to be redrawn
        DrawManager.updateAll = true;

        MainActivity.Debug("Creating super big arrays...\n");

        //Generation initialize
        NewAgeList.initNewAgeList(sizeX * sizeY * 8);
        MutableInt newAgePtr = new MutableInt(0);
        GenerationBufferData.initGenerationBufferData(sizeY, sizeX);

        //Inserting inniter before last (last being islands, inserted one being land deformation)
        GenerationInniter last = generationInniters.getLast();
        generationInniters.removeLast();
        GenerationInniter preLast = generationInniters.getLast();
        generationInniters.removeLast();
        generationInniters.add(new GenerationInniter(-1, -1, minWaterMassPerDeformation, maxWaterMassPerDeformation, LAND, WATER));
        generationInniters.getLast().isDeformation = true;
        generationInniters.add(preLast);
        generationInniters.add(last);


        int counter = 0;
        for (GenerationInniter inniter : generationInniters) {
            LinkedList<Point> seeds = new LinkedList<Point>();
            MainActivity.Debug("Placing seeds #" + counter + "...\n");
            PlaceSeeds(seeds, inniter);
            MainActivity.Debug("Growing seeds #" + counter++ + "...\n");
            GrowSeeds(seeds, inniter, newAgePtr, sizeY, sizeX);
        }
        Smoothing(newAgePtr, sizeY, sizeX);
        DivideIntoRegions(sizeY, sizeX, newAgePtr);
        CreateCountries();
    }


    public static class GenerationInniter {
        public int minSeeds, maxSeeds;
        public float minProc, maxProc;
        public int typeToTransformInto, typeToTransformFrom;
        public boolean isDeformation = false;

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
