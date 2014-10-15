/**
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
 */
package org.graylog2.restclient.lib.metrics;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Histogram extends Metric {

    private final double count;

    private final double standardDeviation;
    private final double minimum;
    private final double maximum;
    private final double mean;
    private final double percentile95th;
    private final double percentile98th;
    private final double percentile99th;


    public Histogram(Map<String, Object> histo, double count) {
        super(MetricType.HISTOGRAM);

        this.count = count;

        this.standardDeviation = ((Number) histo.get("std_dev")).doubleValue();
        this.minimum = ((Number) histo.get("min")).doubleValue();
        this.maximum = ((Number) histo.get("max")).doubleValue();
        this.mean = ((Number) histo.get("mean")).doubleValue();
        this.percentile95th = ((Number) histo.get("95th_percentile")).doubleValue();
        this.percentile98th = ((Number) histo.get("98th_percentile")).doubleValue();
        this.percentile99th = ((Number) histo.get("99th_percentile")).doubleValue();
    }

    public double getCount() {
        return count;
    }

    public double getStandardDeviation() {
        return standardDeviation;
    }

    public double getMinimum() {
        return minimum;
    }

    public double getMaximum() {
        return maximum;
    }

    public double getMean() {
        return mean;
    }

    public double getPercentile95th() {
        return percentile95th;
    }

    public double getPercentile98th() {
        return percentile98th;
    }

    public double getPercentile99th() {
        return percentile99th;
    }
}
