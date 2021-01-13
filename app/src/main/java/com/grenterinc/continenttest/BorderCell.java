package com.grenterinc.continenttest;


public class BorderCell {
    public int urgesToActivate = 0;
    public int y, x;
    public int id;

    public BorderCell(int x, int y) {
        id = Cell.getIdByCoords(y, x);
        this.y = y;
        this.x = x;
    }

    public void activate() {
        //Overlapping activation system. If non-zero - draw, else - not draw.
        urgesToActivate++;
        Cell.setVisibleBorder(id, true);
    }

    public void deActivate() {
        urgesToActivate--;
        if (urgesToActivate == 0) {
            Cell.setVisibleBorder(id, false);
        }
    }
}
