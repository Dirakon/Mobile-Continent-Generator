package com.grenterinc.continenttest;

import android.os.Build;

import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import androidx.annotation.RequiresApi;

import static com.grenterinc.continenttest.Cell.LAND;
import static com.grenterinc.continenttest.Cell.RIVER;
import static com.grenterinc.continenttest.Cell.goingAroundWithFunc9;
import static com.grenterinc.continenttest.DrawManager.borderForUpdate;
import static com.grenterinc.continenttest.Region.regions;
import static com.grenterinc.continenttest.TerrainType.getTerrainTypeByDeepness;

public class BorderWithRegion {
    public LinkedList<BorderCell> cells = new LinkedList<BorderCell>();
    public int regionId, neighbourId;

    public boolean riverHere = false;
    public boolean isDrawn = false;

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static BorderWithRegion makeBorderRiver(BorderWithRegion bord) {

        //Atomic reference to reference during lambda execution
        AtomicReference<BorderWithRegion> nextOne = new AtomicReference<BorderWithRegion>();
        final int changeOwnerChance = 25;//  const % to change next river's region for randomness

        //Both regions have river now
        regions[bord.regionId].hasRiver = regions[bord.neighbourId].hasRiver = true;
        bord.riverHere = true;

        final BorderWithRegion finalBord = bord;

        //Checking lambda -- will be checking each surrounding cell for every cell in the border
        BiFunction<Integer, Integer, Void> checkFunction = (Integer y, Integer x) -> {
            int id = Cell.getIdByCoords(y, x);
            if (
                    (Cell.getBorderPartOfCell(id) //Cell in question is on the border
                            && Cell.getRegionOfCell(id) != finalBord.regionId //Cell is from different border
                            && Cell.getRegionOfCell(id) != finalBord.neighbourId//COMPLETELY different border
                            && Cell.getTypeOfCell(id) == LAND //It's land
                            && getTerrainTypeByDeepness(regions[Cell.getRegionOfCell(id)].deepness).canRiverSpawn) //River can spawn on that terrain
                            && (nextOne.get() == null || new Random().nextInt(100) % 100 > changeOwnerChance) //Random is on our side
            ) {
                //Look for all borders from that cell's region
                for (BorderWithRegion bored : regions[Cell.getRegionOfCell(id)].borders) {
                    //If we find that we know one of it's border's master, we find where river spreads next~!
                    if (bored.neighbourId == finalBord.regionId || bored.neighbourId == finalBord.neighbourId) {
                        nextOne.set(bored);
                        return null;
                    }
                }
            }
            return null;
        };

        //Check all border cells for the next border for river to spread
        for (BorderCell cell : bord.cells) {
            Cell.setTypeOfCell(cell.id, RIVER);
            goingAroundWithFunc9(checkFunction, cell.y, cell.x);
        }
        for (BorderWithRegion anbord : regions[bord.neighbourId].borders) {
            if (anbord.neighbourId == bord.regionId) {
                bord = anbord;
                break;
            }
        }

        //Don't forget to show that this border is cell now.
        bord.riverHere = true;

        //Going thru next border's master's cells
        for (BorderCell cell : bord.cells) {
            Cell.setTypeOfCell(cell.id, RIVER);
            goingAroundWithFunc9(checkFunction, cell.y, cell.x);
        }
        return nextOne.get();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void setDraw(boolean state) {
        //No need to redraw if nothing changes
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
        //Draw manager does the rest, alright...
        borderForUpdate.push(cells);
    }

}
