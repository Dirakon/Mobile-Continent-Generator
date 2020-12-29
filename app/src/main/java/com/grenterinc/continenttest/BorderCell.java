package com.grenterinc.continenttest;

import static com.grenterinc.continenttest.Cell.world;

public class BorderCell {
    public int urgesToActivate = 0;
    public int y, x;

    public BorderCell(int x, int y) {
        this.y = y;
        this.x = x;
    }

    public void activate() {
        //Overlapping activation system. If non-zero - draw, else - not draw.
        urgesToActivate++;
        world[y][x].visibleBorder = true;
    }

    public void deActivate() {
        urgesToActivate--;
        if (urgesToActivate == 0) {
            world[y][x].visibleBorder = false;
        }
    }
}
