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
