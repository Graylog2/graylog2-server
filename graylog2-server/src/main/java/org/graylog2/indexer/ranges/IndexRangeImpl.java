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
package org.graylog2.indexer.ranges;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.joda.time.DateTime;

@AutoValue
@JsonAutoDetect
public abstract class IndexRangeImpl implements IndexRange {
    @JsonProperty
    public abstract String indexName();

    @JsonProperty
    public abstract DateTime begin();

    @JsonProperty
    public abstract DateTime end();

    @JsonProperty
    public abstract DateTime calculatedAt();

    @JsonProperty("took_ms")
    public abstract int calculationDuration();

    public static IndexRange create(String indexName,
                                    DateTime begin,
                                    DateTime end,
                                    DateTime calculatedAt,
                                    int calculationDuration) {
        return new AutoValue_IndexRangeImpl(indexName, begin, end, calculatedAt, calculationDuration);
    }
}
