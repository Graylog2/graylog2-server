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
package org.graylog2.rest.resources.search.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.results.ResultMessage;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Set;

@JsonAutoDetect
@AutoValue
public abstract class SearchResponse {
    @JsonProperty
    public abstract String query();

    @JsonProperty
    public abstract String builtQuery();

    @JsonProperty
    public abstract Set<IndexRange> usedIndices();

    @JsonProperty
    public abstract List<ResultMessage> messages();

    @JsonProperty
    public abstract Set<String> fields();

    @JsonProperty
    public abstract long time();

    @JsonProperty
    public abstract long totalResults();

    @JsonProperty
    public abstract DateTime from();

    @JsonProperty
    public abstract DateTime to();

    public static SearchResponse create(String query,
                                        String builtQuery,
                                        Set<IndexRange> usedIndices,
                                        List<ResultMessage> messages,
                                        Set<String> fields,
                                        long time,
                                        long totalResults,
                                        DateTime from,
                                        DateTime to) {
        return new AutoValue_SearchResponse(query, builtQuery, usedIndices, messages,
                fields, time, totalResults, from, to);
    }
}
