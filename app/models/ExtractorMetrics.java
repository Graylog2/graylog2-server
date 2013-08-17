/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package models;

import models.api.responses.metrics.RateMetricsResponse;
import models.api.responses.metrics.TimerMetricsResponse;
import models.api.responses.metrics.TimerRateMetricsResponse;

import java.text.DecimalFormat;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class ExtractorMetrics {

    public enum TimingUnit {
        MICROSECONDS
    }

    DecimalFormat df = new DecimalFormat("#.##");

    private Meter meter;

    private Timing totalTiming;
    private Timing converterTiming;

    public ExtractorMetrics(TimerRateMetricsResponse total, TimerRateMetricsResponse converters) {
        if (total.durationUnit != null) {
            this.totalTiming = new Timing(total.time, TimingUnit.valueOf(total.durationUnit.toUpperCase()));
        }

        if (converters.durationUnit != null) {
            this.converterTiming = new Timing(converters.time, TimingUnit.valueOf(converters.durationUnit.toUpperCase()));
        }

        if (total.rate == null) {
            this.meter = null;
        } else {
            this.meter = new Meter(total.rate);
        }
    }

    public Timing getTotalTiming() {
        return totalTiming;
    }

    public Timing getConverterTiming() {
        return converterTiming;
    }

    public Meter getMeter() {
        return meter;
    }

    public class Timing {

        private final long standardDeviation;
        private final long minimum;
        private final long maximum;
        private final long mean;
        private final long percentile95th;
        private final long percentile98th;
        private final long percentile99th;

        public Timing(TimerMetricsResponse t, TimingUnit durationUnit) {
            if (!durationUnit.equals(TimingUnit.MICROSECONDS)) {
                throw new RuntimeException("Extractor timings must be in microseconds.");
            }

            this.standardDeviation = t.stdDev;
            this.minimum = t.min;
            this.maximum = t.max;
            this.mean = t.mean;
            this.percentile95th = t.percentile95th;
            this.percentile98th = t.percentile98th;
            this.percentile99th = t.percentile99th;
        }

        public long getStandardDeviation() {
            return standardDeviation;
        }

        public long getMinimum() {
            return minimum;
        }

        public long getMaximum() {
            return maximum;
        }

        public long getMean() {
            return mean;
        }

        public long get95thPercentile() {
            return percentile95th;
        }

        public long get98thPercentile() {
            return percentile98th;
        }

        public long get99thPercentile() {
            return percentile99th;
        }
    }

    public class Meter {

        public final long total;
        public final double mean;
        public final double oneMinute;
        public final double fiveMinute;
        public final double fifteenMinute;

        public Meter(RateMetricsResponse rate) {
            this.total = rate.total;
            this.mean = rate.mean;
            this.oneMinute = rate.oneMinute;
            this.fiveMinute = rate.fiveMinute;
            this.fifteenMinute = rate.fifteenMinute;
        }

        public long getTotal() {
            return total;
        }

        public double getMean() {
            return mean;
        }

        public double getOneMinute() {
            return oneMinute;
        }

        public double getFiveMinute() {
            return fiveMinute;
        }

        public double getFifteenMinute() {
            return fifteenMinute;
        }

        public String getMeanFormatted() {
            return df.format(mean);
        }

        public String getOneMinuteFormatted() {
            return df.format(oneMinute);
        }

        public String getFiveMinuteFormatted() {
            return df.format(fiveMinute);
        }

        public String getFifteenMinuteFormatted() {
            return df.format(fifteenMinute);
        }

    }

}
