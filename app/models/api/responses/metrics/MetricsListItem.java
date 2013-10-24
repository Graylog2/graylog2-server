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
        Metric.TYPE type = Metric.TYPE.valueOf((String) metric.get("type"));

        switch (type) {
            case TIMER:
                return new Timer(metric, Timer.Unit.valueOf((String) metric.get("unit")));
            case METER:
                return new Meter(metric);
            case GAUGE:
                break;
        }

        throw new RuntimeException("Could not map metric to type. Type was: [" + metric.get("type") + "]");
    }

}
