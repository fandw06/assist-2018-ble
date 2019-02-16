package com.dawei.assist_2018.plot;

/**
 * Created by Dawei on 2/15/2017.
 */
public class CalibrateADC implements Calibrate {

    private static double FULL_SCALE = 0.5;
    private static double NUMBER_OF_LEVELS = 256.0;

    @Override
    public double calibrate(byte[] rawData) {
        if (rawData.length != 1)
            return -1;
        int val = 0;
        if (rawData[0] < 0)
            val = rawData[0]+256;
        else
            val = rawData[0];
        return (double)val * FULL_SCALE/NUMBER_OF_LEVELS;
    }

    public double calibrate(byte rawData) {
        int val = 0;
        if (rawData < 0)
            val = rawData + 256;
        else
            val = rawData;
        return (double)val * FULL_SCALE/NUMBER_OF_LEVELS;
    }
}
