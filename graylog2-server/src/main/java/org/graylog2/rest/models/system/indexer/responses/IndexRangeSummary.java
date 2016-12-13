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
import org.joda.time.DateTime;

import javax.annotation.Nullable;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class IndexRangeSummary {
    @JsonProperty("index_name")
    public abstract String indexName();

    @JsonProperty("begin")
    public abstract DateTime begin();

    @JsonProperty("end")
    public abstract DateTime end();

    @Nullable @JsonProperty("calculated_at")
    public abstract DateTime calculatedAt();

    @JsonProperty("took_ms")
    public abstract int calculationTookMs();

    @JsonCreator
    public static IndexRangeSummary create(@JsonProperty("index_name") String indexName,
                                           @JsonProperty("begin") DateTime begin,
                                           @JsonProperty("end") DateTime end,
                                           @Nullable @JsonProperty("calculated_at") DateTime calculatedAt,
                                           @JsonProperty("took_ms") int calculationTookMs) {
        return new AutoValue_IndexRangeSummary(indexName, begin, end, calculatedAt, calculationTookMs);
    }
}
