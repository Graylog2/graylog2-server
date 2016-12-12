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
package org.graylog2.rest.models.system.indexer.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class IndexSummary {
    @JsonProperty("size")
    @Nullable
    public abstract IndexSizeSummary size();

    @JsonProperty("range")
    @Nullable
    public abstract IndexRangeSummary range();

    @JsonProperty("is_deflector")
    public abstract boolean isDeflector();

    @JsonProperty("is_closed")
    public abstract boolean isClosed();

    @JsonProperty("is_reopened")
    public abstract boolean isReopened();

    @JsonCreator
    public static IndexSummary create(@JsonProperty("size") @Nullable IndexSizeSummary size,
                                      @JsonProperty("range") @Nullable IndexRangeSummary range,
                                      @JsonProperty("is_deflector") boolean isDeflector,
                                      @JsonProperty("is_closed") boolean isClosed,
                                      @JsonProperty("is_reopened") boolean isReopened) {
        return new AutoValue_IndexSummary(size, range, isDeflector, isClosed, isReopened);
    }
}
