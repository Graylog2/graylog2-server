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
package org.graylog2.restclient.models;

import org.graylog2.restclient.lib.metrics.Meter;
import org.graylog2.restclient.lib.metrics.Timer;
import org.graylog2.restclient.models.api.responses.metrics.TimerRateMetricsResponse;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class ExtractorMetrics {

    private Meter meter;

    private Timer totalTiming;
    private Timer converterTiming;

    public ExtractorMetrics(TimerRateMetricsResponse total, TimerRateMetricsResponse converters) {
        if (total.durationUnit != null) {
            this.totalTiming = new Timer(total.time, Timer.Unit.valueOf(total.durationUnit.toUpperCase()));
        }

        if (converters.durationUnit != null) {
            this.converterTiming = new Timer(converters.time, Timer.Unit.valueOf(converters.durationUnit.toUpperCase()));
        }

        if (total.rate == null) {
            this.meter = null;
        } else {
            this.meter = new Meter(total.rate);
        }
    }

    public Timer getTotalTiming() {
        return totalTiming;
    }

    public Timer getConverterTiming() {
        return converterTiming;
    }

    public Meter getMeter() {
        return meter;
    }

}
