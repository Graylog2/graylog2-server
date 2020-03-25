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
package org.graylog.plugins.views.search;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.engine.QueryExecutionStats;
import org.graylog.plugins.views.search.errors.SearchError;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = AutoValue_QueryResult.Builder.class)
public abstract class QueryResult {

    public static QueryResult incomplete() {
        return QueryResult.emptyResult().toBuilder().state(State.INCOMPLETE).build();
    }

    @JsonProperty
    public abstract Query query();

    @JsonProperty("execution_stats")
    public abstract QueryExecutionStats executionStats();

    @JsonProperty("search_types")
    public abstract Map<String, SearchType.Result> searchTypes();

    @JsonProperty("errors")
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    public abstract Set<SearchError> errors();

    @JsonProperty("state")
    public abstract State state();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    public static QueryResult emptyResult() {
        return builder().searchTypes(Collections.emptyMap()).query(Query.emptyRoot()).build();
    }

    public static QueryResult failedQueryWithError(Query query, SearchError error) {
        return emptyResult().toBuilder()
                .query(query)
                .errors(Collections.singleton(error))
                .state(State.FAILED)
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_QueryResult.Builder()
                    .state(State.COMPLETED)
                    .executionStats(QueryExecutionStats.empty());
        }

        public abstract Builder query(Query query);

        public abstract Builder executionStats(QueryExecutionStats stats);

        public abstract Builder searchTypes(Map<String, SearchType.Result> results);

        public abstract Builder errors(@Nullable Set<SearchError> errors);

        public abstract Builder state(State state);

        public abstract QueryResult build();
    }

    public enum State {
        INCOMPLETE,
        FAILED,
        COMPLETED
    }
}
