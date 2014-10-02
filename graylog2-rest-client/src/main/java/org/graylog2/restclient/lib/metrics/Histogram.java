/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
