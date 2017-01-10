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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.rest.models.metrics.responses.TimerRateMetricsResponse;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class ExtractorMetrics {
    @JsonProperty
    public abstract TimerRateMetricsResponse total();

    @JsonProperty
    public abstract TimerRateMetricsResponse condition();

    @JsonProperty
    public abstract TimerRateMetricsResponse execution();

    @JsonProperty
    public abstract TimerRateMetricsResponse converters();

    @JsonProperty
    public abstract long conditionHits();

    @JsonProperty
    public abstract long conditionMisses();

    public static ExtractorMetrics create(TimerRateMetricsResponse total,
                                          TimerRateMetricsResponse condition,
                                          TimerRateMetricsResponse execution,
                                          TimerRateMetricsResponse converters,
                                          long conditionHits,
                                          long conditionMisses) {
        return new AutoValue_ExtractorMetrics(total, condition, execution, converters, conditionHits, conditionMisses);
    }
}
