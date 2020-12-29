package com.grenterinc.continenttest;

import android.os.Build;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import androidx.annotation.RequiresApi;

import static com.grenterinc.continenttest.Cell.LAND;
import static com.grenterinc.continenttest.Cell.RIVER;
import static com.grenterinc.continenttest.Cell.goingAroundWithFunc9;
import static com.grenterinc.continenttest.Cell.world;
import static com.grenterinc.continenttest.DrawManager.borderForUpdate;
import static com.grenterinc.continenttest.Region.regions;
import static com.grenterinc.continenttest.TerrainType.getTerrainTypeByDeepness;

public class BorderWithRegion {
    public ArrayList<BorderCell> cells = new ArrayList<BorderCell>();
    public int regionId, neighbourId;

    public boolean riverHere = false;
    public boolean isDrawn = false;

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static BorderWithRegion makeBorderRiver(BorderWithRegion bord) {
        AtomicReference<BorderWithRegion> nextOne = null;
        final int changeOwnerChance = 25;//  %
        regions[bord.regionId].hasRiver = regions[bord.neighbourId].hasRiver = true;
        bord.riverHere = true;
        //std::function<void(int, int)> checkFunction = [changeOwnerChance, &bord, &nextOne](int y, int x)
        final BorderWithRegion finalBord = bord;
        BiFunction<Integer, Integer, Void> checkFunction = (Integer y, Integer x) -> {//LAMBDA FUNCTION VOID (INT,INT) checkFunction;
            if ((world[y][x].borderPart && world[y][x].region != finalBord.regionId && world[y][x].region != finalBord.neighbourId && world[y][x].type == LAND && getTerrainTypeByDeepness(regions[world[y][x].region].deepness).canRiverSpawn) && (nextOne.get() == null || new Random().nextInt(100) % 100 > changeOwnerChance)) {
                for (BorderWithRegion bored : regions[world[y][x].region].borders) {
                    if (bored.neighbourId == finalBord.regionId || bored.neighbourId == finalBord.neighbourId) {
                        nextOne.set(bored);
                        return null;
                    }
                }
            }
            return null;
        };
        for (BorderCell cell : bord.cells) {
            world[cell.y][cell.x].type = RIVER;
            goingAroundWithFunc9(checkFunction, cell.y, cell.x);
        }
        for (BorderWithRegion anbord : regions[bord.neighbourId].borders) {
            if (anbord.neighbourId == bord.regionId) {
                bord = anbord;
                break;
            }
        }
        bord.riverHere = true;
        for (BorderCell cell : bord.cells) {
            world[cell.y][cell.x].type = RIVER;
            goingAroundWithFunc9(checkFunction, cell.y, cell.x);
        }
        return nextOne.get();
    }

    public void setDraw(boolean state) {
        if (state == isDrawn)
            return;
        isDrawn = state;
        if (isDrawn) {
            //Go draw it
            for (BorderCell var : cells) {
                var.activate();
            }
        } else {
            //Go undraw it
            for (BorderCell var : cells) {
                var.deActivate();
            }
        }
        borderForUpdate.push(cells);
    }

}
