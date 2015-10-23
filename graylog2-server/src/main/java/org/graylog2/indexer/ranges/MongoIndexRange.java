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
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mongojack.Id;

import javax.annotation.Nullable;


@AutoValue
@JsonAutoDetect
public abstract class MongoIndexRange implements IndexRange {
    @Id
    @Nullable
    @JsonProperty("_id")
    public abstract ObjectId id();

    @JsonProperty(FIELD_INDEX_NAME)
    public abstract String indexName();

    public abstract DateTime begin();

    public abstract DateTime end();

    public abstract DateTime calculatedAt();

    @JsonProperty(FIELD_TOOK_MS)
    public abstract int calculationDuration();

    @JsonProperty(FIELD_BEGIN)
    private long beginMillis() {
        return begin().getMillis();
    }

    @JsonProperty(FIELD_END)
    private long endMillis() {
        return end().getMillis();
    }

    @JsonProperty(FIELD_CALCULATED_AT)
    private long calculatedAtMillis() {
        return calculatedAt().getMillis();
    }

    public static MongoIndexRange create(ObjectId id,
                                         String indexName,
                                         DateTime begin,
                                         DateTime end,
                                         DateTime calculatedAt,
                                         int calculationDuration) {
        return new AutoValue_MongoIndexRange(id, indexName, begin, end, calculatedAt, calculationDuration);
    }

    @JsonCreator
    public static MongoIndexRange create(@JsonProperty("_id") @Id @Nullable ObjectId id,
                                         @JsonProperty(FIELD_INDEX_NAME) String indexName,
                                         @JsonProperty(FIELD_BEGIN) long beginMillis,
                                         @JsonProperty(FIELD_END) long endMillis,
                                         @JsonProperty(FIELD_CALCULATED_AT) long calculatedAtMillis,
                                         @JsonProperty(FIELD_TOOK_MS) int calculationDuration) {
        final DateTime begin = new DateTime(beginMillis, DateTimeZone.UTC);
        final DateTime end = new DateTime(endMillis, DateTimeZone.UTC);
        final DateTime calculatedAt = new DateTime(calculatedAtMillis, DateTimeZone.UTC);
        return new AutoValue_MongoIndexRange(id, indexName, begin, end, calculatedAt, calculationDuration);
    }

    public static MongoIndexRange create(String indexName,
                                         DateTime begin,
                                         DateTime end,
                                         DateTime calculatedAt,
                                         int calculationDuration) {
        return create(null, indexName, begin, end, calculatedAt, calculationDuration);
    }

    public static MongoIndexRange create(IndexRange indexRange) {
        return create(
                indexRange.indexName(),
                indexRange.begin(),
                indexRange.end(),
                indexRange.calculatedAt(),
                indexRange.calculationDuration());
    }
}