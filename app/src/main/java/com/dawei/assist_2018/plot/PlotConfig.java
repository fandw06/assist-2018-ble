package com.dawei.assist_2018.plot;

/**
 * Created by Dawei on 2/15/2017.
 */
public class PlotConfig {

    public int REDRAW_FREQ;
    public int BYTES_PER_SAMPLE;
    public int NUMBER_OF_SERIES;
    public int RES_ID;

    // Configurations for data series.
    public String NAME[];
    public int XML_ID[];
    // Configurations for plot.
    public double DOMAIN_BOUNDARY[];
    public double DOMAIN_INC;
    public double RANGE_BOUNDARY[];
    public double RANGE_INC;


    private PlotConfig(){}

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        /* Default configurations. */
        public int REDRAW_FREQ = 20;
        public int BYTES_PER_SAMPLE = 1;
        public int NUMBER_OF_SERIES = 1;
        public int RES_ID;
        public int XML_ID[];

        // Configurations for data series.
        public String NAME[];

        // Configurations for plot.
        public double DOMAIN_BOUNDARY[] = {0, 400};
        public double DOMAIN_INC = 50;
        public double RANGE_BOUNDARY[] = {0, 100};
        public double RANGE_INC = 50;

        private Builder() {}

        public Builder setRedrawFreq(int val) {
            this.REDRAW_FREQ = val;
            return this;
        }

        public Builder setBytesPerSample(int val) {
            this.BYTES_PER_SAMPLE = val;
            return this;
        }

        public Builder setNumOfSeries(int val) {
            this.NUMBER_OF_SERIES = val;
            return this;
        }

        public Builder setResID(int val) {
            this.RES_ID = val;
            return this;
        }

        public Builder setXmlID(int val[]) {
            this.XML_ID = val;
            return this;
        }

        public Builder setName(String s[]) {
            this.NAME = s;
            return this;
        }

        public Builder setDomainBoundary(double d[]) {
            this.DOMAIN_BOUNDARY = d;
            return this;
        }

        public Builder setRangeBoundary(double d[]) {
            this.RANGE_BOUNDARY = d;
            return this;
        }

        public Builder setDomainInc(double inc) {
            this.DOMAIN_INC = inc;
            return this;
        }

        public Builder setRangeInc(double inc) {
            this.RANGE_INC = inc;
            return this;
        }

        public PlotConfig build() {
            PlotConfig config = new PlotConfig();
            config.REDRAW_FREQ = this.REDRAW_FREQ;
            config.BYTES_PER_SAMPLE = this.BYTES_PER_SAMPLE;
            config.NUMBER_OF_SERIES = this.NUMBER_OF_SERIES;
            config.RES_ID = this.RES_ID;
            config.XML_ID = this.XML_ID;
            config.NAME = this.NAME;
            config.DOMAIN_BOUNDARY = this.DOMAIN_BOUNDARY;
            config.DOMAIN_INC = this.DOMAIN_INC;
            config.RANGE_BOUNDARY = this.RANGE_BOUNDARY;
            config.RANGE_INC = this.RANGE_INC;
            return config;
        }
    }

}
