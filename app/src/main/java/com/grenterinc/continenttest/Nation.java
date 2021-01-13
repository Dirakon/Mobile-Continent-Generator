package com.grenterinc.continenttest;

import android.os.Build;

import java.util.ArrayList;

import androidx.annotation.RequiresApi;

import static com.grenterinc.continenttest.DrawManager.DRAW_COUNTRIES;
import static com.grenterinc.continenttest.DrawManager.drawType;
import static com.grenterinc.continenttest.DrawManager.regionsForUpdate;
import static com.grenterinc.continenttest.Std.inBetweenTwoFloats;

public class Nation {
    public ArrayList<Region> regions = new ArrayList<Region>();
    public float colorR, colorG, colorB;
    public float livabilityTreshold = 2.6f;

    public Nation() {

        //Set random colors
        colorR = inBetweenTwoFloats(0, 1);
        colorG = inBetweenTwoFloats(0, 1);
        colorB = inBetweenTwoFloats(0, 1);

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void annex(Region region) {

        if (region.father != null) {
            //Remove this region from list of his owner's regions
            region.father.regions.remove(region);
        }

        //Make this region mine
        region.father = this;
        regions.add(region);

        //If we're drawing countries, need to update some stuff.
        if (drawType == DRAW_COUNTRIES) {
            //Every border of this region
            for (BorderWithRegion bord : region.borders) {
                //We set sufficient visibility
                boolean setter = this != Region.regions[bord.neighbourId].father;
                bord.setDraw(setter);

                //Looking for the same border from different perspective
                for (BorderWithRegion bord2 : Region.regions[bord.neighbourId].borders) {
                    if (bord2.neighbourId == bord.regionId) {
                        //Set sufficient visibility
                        bord2.setDraw(setter);

                        //Draw manager will do the rest *COMMENTED FOR THE TIME BEING, WHY WOULD WE NEED TO DO THIS IF IT'S ALREADY IN setDraw(bool)???*
                        //  borderForUpdate.push(bord2.cells);
                        //  borderForUpdate.push(bord.cells);
                        break;
                    }
                }
            }
            regionsForUpdate.push(region);
        }


    }
}
