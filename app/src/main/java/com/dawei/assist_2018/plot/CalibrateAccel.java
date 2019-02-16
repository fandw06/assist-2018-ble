package com.dawei.assist_2018.plot;

/**
 * Created by Dawei on 2/15/2017.
 */
public class CalibrateAccel implements Calibrate {

    private static double FULL_SCALE = 2.0;
    private static double NUMBER_OF_LEVELS = 256;

    @Override
    public double calibrate(byte[] rawData) {
        return (double)rawData[0] * FULL_SCALE * 2/NUMBER_OF_LEVELS;
    }

    public double calibrate(byte rawData) {
        return (double)rawData * FULL_SCALE * 2/NUMBER_OF_LEVELS;
    }
}
