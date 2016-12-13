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
package org.graylog2.rest.models.system.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.joda.time.Period;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class TimeBasedRotationStrategyResponse implements DeflectorConfigResponse {
    @JsonProperty("max_time_per_index")
    public abstract Period maxTimePerIndex();

    public static TimeBasedRotationStrategyResponse create(@JsonProperty(TYPE_FIELD) String type,
                                                           @JsonProperty("max_number_of_indices") int maxNumberOfIndices,
                                                           @JsonProperty("max_time_per_index") Period maxTimePerIndex) {
        return new AutoValue_TimeBasedRotationStrategyResponse(type, maxNumberOfIndices, maxTimePerIndex);
    }

    public static TimeBasedRotationStrategyResponse create(@JsonProperty("max_number_of_indices") int maxNumberOfIndices,
                                                           @JsonProperty("max_time_per_index") Period maxTimePerIndex) {
        return create(TimeBasedRotationStrategyResponse.class.getCanonicalName(), maxNumberOfIndices, maxTimePerIndex);
    }
}
