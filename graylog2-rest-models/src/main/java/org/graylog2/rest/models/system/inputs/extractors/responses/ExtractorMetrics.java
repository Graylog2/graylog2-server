/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest.models.system.inputs.extractors.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.rest.models.metrics.responses.TimerRateMetricsResponse;

import java.util.Map;

@AutoValue
@JsonAutoDetect
public abstract class ExtractorMetrics {

    @JsonProperty
    public abstract TimerRateMetricsResponse total();

    @JsonProperty
    public abstract TimerRateMetricsResponse converters();

    @JsonCreator
    public static ExtractorMetrics create(@JsonProperty("total") TimerRateMetricsResponse total,
                                          @JsonProperty("converters") TimerRateMetricsResponse converters) {
        return new AutoValue_ExtractorMetrics(total, converters);
    }
}
