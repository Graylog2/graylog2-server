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

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class SizeBasedRotationStrategyResponse implements DeflectorConfigResponse {
    @JsonProperty("max_size_per_index")
    public abstract long maxSizePerIndex();

    public static SizeBasedRotationStrategyResponse create(@JsonProperty(TYPE_FIELD) String type,
                                                           @JsonProperty("max_number_of_indices") int maxNumberOfIndices,
                                                           @JsonProperty("max_size_per_index") long maxSizePerIndex) {
        return new AutoValue_SizeBasedRotationStrategyResponse(type, maxNumberOfIndices, maxSizePerIndex);
    }

    public static SizeBasedRotationStrategyResponse create(@JsonProperty("max_number_of_indices") int maxNumberOfIndices,
                                                           @JsonProperty("max_size_per_index") long maxSizePerIndex) {
        return create(SizeBasedRotationStrategyResponse.class.getCanonicalName(), maxNumberOfIndices, maxSizePerIndex);
    }
}
