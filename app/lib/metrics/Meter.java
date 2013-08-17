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
package lib.metrics;

import models.api.responses.metrics.RateMetricsResponse;

import java.text.DecimalFormat;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Meter {

    DecimalFormat df = new DecimalFormat("#.##");

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
