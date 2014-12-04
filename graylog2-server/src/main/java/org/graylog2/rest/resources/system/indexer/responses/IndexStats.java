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
package org.graylog2.rest.resources.system.indexer.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@JsonAutoDetect
@AutoValue
public abstract class IndexStats {
    @JsonProperty
    public abstract TimeAndTotalStats flush();

    @JsonProperty
    public abstract TimeAndTotalStats get();

    @JsonProperty
    public abstract TimeAndTotalStats index();

    @JsonProperty
    public abstract TimeAndTotalStats merge();

    @JsonProperty
    public abstract TimeAndTotalStats refresh();

    @JsonProperty
    public abstract TimeAndTotalStats searchQuery();

    @JsonProperty
    public abstract TimeAndTotalStats searchFetch();

    @JsonProperty
    public abstract long openSearchContexts();

    @JsonProperty
    public abstract long store_size_bytes();

    @JsonProperty
    public abstract long segments();

    @JsonProperty
    public abstract DocsStats documents();

    public static IndexStats create(TimeAndTotalStats flush,
                                    TimeAndTotalStats get,
                                    TimeAndTotalStats index,
                                    TimeAndTotalStats merge,
                                    TimeAndTotalStats refresh,
                                    TimeAndTotalStats searchQuery,
                                    TimeAndTotalStats searchFetch,
                                    long openSearchContexts,
                                    long store_size_bytes,
                                    long segments,
                                    DocsStats documents) {
        return new AutoValue_IndexStats(flush, get, index, merge, refresh, searchQuery, searchFetch,
                openSearchContexts, store_size_bytes, segments, documents);
    }

    @JsonAutoDetect
    @AutoValue
    public static abstract class DocsStats {
        @JsonProperty
        public abstract long count();

        @JsonProperty
        public abstract long deleted();

        public static DocsStats create(long count, long deleted) {
            return new AutoValue_IndexStats_DocsStats(count, deleted);
        }
    }

    @JsonAutoDetect
    @AutoValue
    public static abstract class TimeAndTotalStats {
        @JsonProperty
        public abstract long total();

        @JsonProperty
        public abstract long timeSeconds();

        public static TimeAndTotalStats create(long total, long timeSeconds) {
            return new AutoValue_IndexStats_TimeAndTotalStats(total, timeSeconds);
        }
    }


}
