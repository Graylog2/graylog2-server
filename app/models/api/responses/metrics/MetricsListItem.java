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
package models.api.responses.metrics;

import com.google.gson.annotations.SerializedName;
import lib.metrics.Meter;
import lib.metrics.Metric;
import lib.metrics.Timer;
import play.Logger;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class MetricsListItem {

    public String name;
    public String type;

    @SerializedName("full_name")
    public String fullName;

    Map<String, Object> metric;

    public Metric getMetric() {
        Metric.TYPE metricType = Metric.TYPE.valueOf(this.type.toUpperCase());

        try {
            switch (metricType) {
                case TIMER:
                    String timeUnit = (String) metric.get("duration_unit");
                    Map<String, Object> timing = (Map<String, Object>) metric.get("time");
                    return new Timer(timing, Timer.Unit.valueOf(timeUnit.toUpperCase()));
                case METER:
                    Map<String, Object> rate = (Map<String, Object>) metric.get("rate");
                    return new Meter(rate);
                case GAUGE:
                    // TODO
Logger.info("AHHHH GAUGE");
                    return null;
                case HISTOGRAM:
                    // TODO (what type is that even?)
Logger.info("AHHHH HISTO");
                    return null;
            }
        } catch(Exception e) {
            Logger.error("Could not parse metric: " + metric.toString(), e);
            throw new RuntimeException("Could not map metric to type. (more information in log) Type was: [" + this.type + "]", e);
        }

        throw new RuntimeException("No such metric type recognized: [" + this.type + "]");
    }

}
