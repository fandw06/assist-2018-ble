package com.dawei.assist_2018.plot;

import android.util.Log;

import com.androidplot.util.Redrawer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYPlot;
import com.dawei.assist_2018.DisplayActivity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Dawei on 2/15/2017.
 */
public class DataPlot {

    private PlotConfig config;
    private DisplayActivity hostActivity;
    private XYPlot xyPlot;
    private List<Number> dataList[];
    private SimpleXYSeries dataSeries[];
    private Calibrate calibrate;
    public Redrawer redrawer;

    public DataPlot(DisplayActivity hostActivity, PlotConfig config, Calibrate calibrate) {
        this.hostActivity = hostActivity;
        this.config = config;
        this.calibrate = calibrate;
        initializePlot();
        redrawer = new Redrawer(xyPlot, config.REDRAW_FREQ, true);
    }

    public void updateDataSeries(byte raw[]){
        int len = raw.length;
        if (len == 0 || len % config.BYTES_PER_SAMPLE != 0)
            return;
        int samples = len / config.BYTES_PER_SAMPLE;
        if (len % config.NUMBER_OF_SERIES != 0)
            return;
        int points = samples / config.NUMBER_OF_SERIES;
        for (int i = 0; i < points; i++) {
            for (int j = 0; j < config.NUMBER_OF_SERIES; j++) {
                double data = calibrate.calibrate(
                        Arrays.copyOfRange(raw,
                                (i * config.NUMBER_OF_SERIES + j) * config.BYTES_PER_SAMPLE,
                                (i * config.NUMBER_OF_SERIES + j) * config.BYTES_PER_SAMPLE + config.BYTES_PER_SAMPLE));
                if (dataSeries[j].size() == config.DOMAIN_BOUNDARY[1])
                    dataSeries[j].removeFirst();
                dataSeries[j].addLast(config.DOMAIN_BOUNDARY[1], data);
            }
        }
    }

    private void initializePlot() {
        xyPlot = (XYPlot) this.hostActivity.findViewById(config.RES_ID);
        dataSeries = new SimpleXYSeries[config.NUMBER_OF_SERIES];
        dataList = new List[config.NUMBER_OF_SERIES];
        for (int i = 0; i< config.NUMBER_OF_SERIES; i++) {
            dataList[i] = new ArrayList<>();
            dataSeries[i] = new SimpleXYSeries(
                    dataList[i],
                    SimpleXYSeries.ArrayFormat.Y_VALS_ONLY,
                    config.NAME[i]);
            Log.d("DEBUG", String.valueOf(config.XML_ID[i]));
            LineAndPointFormatter format = new LineAndPointFormatter(this.hostActivity, config.XML_ID[i]);
            xyPlot.addSeries(dataSeries[i], format);
        }
        xyPlot.getGraph().setDrawMarkersEnabled(true);
        xyPlot.setDomainBoundaries(config.DOMAIN_BOUNDARY[0], config.DOMAIN_BOUNDARY[1], BoundaryMode.FIXED);
        xyPlot.setDomainStep(StepMode.INCREMENT_BY_VAL, config.DOMAIN_INC);
        xyPlot.setRangeBoundaries(config.RANGE_BOUNDARY[0], config.RANGE_BOUNDARY[1], BoundaryMode.FIXED);
        xyPlot.setRangeStep(StepMode.INCREMENT_BY_VAL, config.RANGE_INC);
        xyPlot.setPlotMargins(0, 0, 0, 0);
    }
}
