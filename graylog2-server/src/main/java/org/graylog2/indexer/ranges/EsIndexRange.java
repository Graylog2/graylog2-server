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

import com.google.auto.value.AutoValue;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.DateTime;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

@AutoValue
@JsonAutoDetect
@Deprecated
public abstract class EsIndexRange implements IndexRange {
    public static final String PREFIX = "gl2_index_range_";
    public static final String FIELD_TOOK_MS = PREFIX + "took_ms";
    public static final String FIELD_CALCULATED_AT = PREFIX + "calculated_at";
    public static final String FIELD_END = PREFIX + "end";
    public static final String FIELD_BEGIN = PREFIX + "begin";
    public static final String FIELD_INDEX_NAME = PREFIX + "index_name";
    public static final String FIELD_STREAM_IDS = PREFIX + "stream_ids";

    @JsonProperty(FIELD_INDEX_NAME)
    @Override
    public abstract String indexName();

    @JsonProperty(FIELD_BEGIN)
    @Override
    public abstract DateTime begin();

    @JsonProperty(FIELD_END)
    @Override
    public abstract DateTime end();

    @JsonProperty(FIELD_CALCULATED_AT)
    @Override
    public abstract DateTime calculatedAt();

    @JsonProperty(FIELD_TOOK_MS)
    @Override
    public abstract int calculationDuration();

    @JsonProperty(FIELD_STREAM_IDS)
    @Override
    @Nullable
    public abstract List<String> streamIds();

    public static EsIndexRange create(String indexName,
                                      DateTime begin,
                                      DateTime end,
                                      DateTime calculatedAt,
                                      int calculationDuration,
                                      List<String> streamIds) {
        return new AutoValue_EsIndexRange(indexName, begin, end, calculatedAt, calculationDuration, streamIds);
    }

    public static EsIndexRange create(String indexName,
                                      DateTime begin,
                                      DateTime end,
                                      DateTime calculatedAt,
                                      int calculationDuration) {
        return create(indexName, begin, end, calculatedAt, calculationDuration, null);
    }
}