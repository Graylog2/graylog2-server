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
package org.graylog2.rest.models.system.buffers.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@AutoValue
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect
public abstract class RingSummary {
    @JsonProperty
    public abstract SingleRingUtilization input();
    @JsonProperty
    @Nullable
    public abstract SingleRingUtilization output();

    @JsonCreator
    public static RingSummary create(@JsonProperty("input") SingleRingUtilization input, @JsonProperty("output") @Nullable SingleRingUtilization output) {
        return new AutoValue_RingSummary(input, output);
    }

    public static RingSummary create(SingleRingUtilization input) {
        return new AutoValue_RingSummary(input, null);
    }
}
