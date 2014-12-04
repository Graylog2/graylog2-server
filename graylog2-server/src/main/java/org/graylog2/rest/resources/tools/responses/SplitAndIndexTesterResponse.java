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
package org.graylog2.rest.resources.tools.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@JsonAutoDetect
@AutoValue
public abstract class SplitAndIndexTesterResponse {
    @JsonProperty
    public abstract boolean successful();

    @JsonProperty
    @Nullable
    public abstract String cut();

    @JsonProperty("begin_index")
    public abstract int beginIndex();

    @JsonProperty("end_index")
    public abstract int endIndex();

    public static SplitAndIndexTesterResponse create(boolean successful, @Nullable String cut, int beginIndex, int endIndex) {
        return new AutoValue_SplitAndIndexTesterResponse(successful, cut, beginIndex, endIndex);
    }
}
