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
package org.graylog2.rest.models.tools.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
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

    @JsonCreator
    public static SplitAndIndexTesterResponse create(@JsonProperty("total") boolean successful,
                                                     @JsonProperty("cut") @Nullable String cut,
                                                     @JsonProperty("begin_index") int beginIndex,
                                                     @JsonProperty("end_index") int endIndex) {
        return new AutoValue_SplitAndIndexTesterResponse(successful, cut, beginIndex, endIndex);
    }
}
