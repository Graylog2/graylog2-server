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
package org.graylog2.rest.resources.search.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;
import org.graylog2.rest.models.system.indexer.responses.IndexRangeSummary;
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
    public abstract Set<IndexRangeSummary> usedIndices();

    @JsonProperty
    public abstract List<ResultMessageSummary> messages();

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
                                        Set<IndexRangeSummary> usedIndices,
                                        List<ResultMessageSummary> messages,
                                        Set<String> fields,
                                        long time,
                                        long totalResults,
                                        DateTime from,
                                        DateTime to) {
        return new AutoValue_SearchResponse(query, builtQuery, usedIndices, messages,
                fields, time, totalResults, from, to);
    }
}
