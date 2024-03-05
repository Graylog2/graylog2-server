/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.views.search.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchJobIdentifier;
import org.graylog.plugins.views.search.errors.SearchError;

import java.util.Map;
import java.util.Set;

@AutoValue
@JsonAutoDetect
@JsonPropertyOrder({"execution", "results"})
public abstract class SearchJobDTO {
    @JsonUnwrapped
    abstract SearchJobIdentifier searchJobIdentifier();

    @JsonProperty("errors")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    abstract Set<SearchError> errors();

    @JsonProperty
    public abstract Map<String, QueryResult> results();

    @JsonProperty
    abstract ExecutionInfo execution();

    public static SearchJobDTO fromSearchJob(SearchJob searchJob) {
        return Builder.create()
                .searchJobIdentifier(searchJob.getSearchJobIdentifier())
                .errors(searchJob.getErrors())
                .results(searchJob.results())
                .execution(ExecutionInfo.fromExecutionInfo(searchJob.execution()))
                .build();
    }

    @AutoValue.Builder
    abstract static class Builder {
        abstract Builder searchJobIdentifier(SearchJobIdentifier searchJobIdentifier);

        abstract Builder errors(Set<SearchError> errors);

        abstract Builder results(Map<String, QueryResult> results);

        abstract Builder execution(ExecutionInfo executionInfo);

        abstract SearchJobDTO build();

        static SearchJobDTO.Builder create() {
            return new AutoValue_SearchJobDTO.Builder();
        }
    }

    @AutoValue
    abstract static class ExecutionInfo {
        @JsonProperty
        abstract boolean done();

        @JsonProperty
        abstract boolean cancelled();

        @JsonProperty("completed_exceptionally")
        abstract boolean hasErrors();

        public static ExecutionInfo fromExecutionInfo(SearchJob.ExecutionInfo execution) {
            return new AutoValue_SearchJobDTO_ExecutionInfo(execution.done, execution.cancelled, execution.hasErrors);
        }
    }
}
