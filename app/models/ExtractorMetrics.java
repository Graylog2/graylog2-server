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

import lib.metrics.Meter;
import lib.metrics.Timing;
import models.api.responses.metrics.RateMetricsResponse;
import models.api.responses.metrics.TimerMetricsResponse;
import models.api.responses.metrics.TimerRateMetricsResponse;

import java.text.DecimalFormat;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class ExtractorMetrics {

    private Meter meter;

    private Timing totalTiming;
    private Timing converterTiming;

    public ExtractorMetrics(TimerRateMetricsResponse total, TimerRateMetricsResponse converters) {
        if (total.durationUnit != null) {
            this.totalTiming = new Timing(total.time, Timing.Unit.valueOf(total.durationUnit.toUpperCase()));
        }

        if (converters.durationUnit != null) {
            this.converterTiming = new Timing(converters.time, Timing.Unit.valueOf(converters.durationUnit.toUpperCase()));
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

}
