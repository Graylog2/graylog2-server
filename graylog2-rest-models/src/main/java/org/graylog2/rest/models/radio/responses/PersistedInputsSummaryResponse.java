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
package org.graylog2.rest.models.radio.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
@JsonAutoDetect
public abstract class PersistedInputsSummaryResponse {
    @JsonProperty
    public abstract int total();
    @JsonProperty
    public abstract List<PersistedInputsResponse> inputs();

    @JsonCreator
    public static PersistedInputsSummaryResponse create(@JsonProperty("total") int total, @JsonProperty("inputs") List<PersistedInputsResponse> inputs) {
        return new AutoValue_PersistedInputsSummaryResponse(total, inputs);
    }

    public static PersistedInputsSummaryResponse create(List<PersistedInputsResponse> inputs) {
        return new AutoValue_PersistedInputsSummaryResponse(inputs.size(), inputs);
    }
}
