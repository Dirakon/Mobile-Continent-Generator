package com.grenterinc.continenttest;

import java.util.Random;

public class Std {
    private Std() {

    }

    public static float inBetweenTwoFloats(float min, float max) {
        return new Random().nextFloat() * (max - min) + min;
    }

    public static int inBetweenTwoInts(int min, int max) {
        return new Random().nextInt(max - min + 1) + min;
    }
}
