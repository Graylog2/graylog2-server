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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchJobIdentifier;
import org.graylog.plugins.views.search.errors.SearchError;
import org.graylog.plugins.views.search.jobs.SearchJobState;
import org.graylog.plugins.views.search.jobs.SearchJobStatus;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.graylog.plugins.views.search.jobs.SearchJobStatus.CANCELLED;
import static org.graylog.plugins.views.search.jobs.SearchJobStatus.ERROR;
import static org.graylog.plugins.views.search.jobs.SearchJobStatus.RUNNING;
import static org.graylog.plugins.views.search.jobs.SearchJobStatus.TIMEOUT;

@JsonPropertyOrder({"execution", "results"})
public record SearchJobDTO(
        @JsonUnwrapped @JsonProperty(access = JsonProperty.Access.READ_ONLY) SearchJobIdentifier searchJobIdentifier,
        @JsonProperty("errors") @JsonInclude(JsonInclude.Include.NON_EMPTY) Set<SearchError> errors,
        @JsonProperty Map<String, QueryResult> results,
        @JsonProperty ExecutionInfo execution) {


    public static SearchJobDTO fromSearchJob(final SearchJob searchJob) {
        final ExecutionInfo executionInfo = searchJob.execution();
        return new SearchJobDTO(
                searchJob.getSearchJobIdentifier(),
                searchJob.getErrors(),
                searchJob.results(),
                executionInfo);
    }

    public static SearchJobDTO fromSearchJobState(final SearchJobState searchJob, final Optional<Search> loadedSearch) {
        //TODO: bring back when deprecated method in DataWarehouseQueryResource is gone
//        if (loadedSearch.isEmpty()) {
//            //TODO: less hardcore error handling?
//            throw new IllegalStateException("Search Job stored in the database references missing Search");
//        }
        final SearchJobStatus status = searchJob.status();
        final ExecutionInfo executionInfo = new ExecutionInfo(
                status != RUNNING,
                status == CANCELLED || status == TIMEOUT,
                status == ERROR
        );
        return new SearchJobDTO(
                searchJob.identifier(),
                searchJob.errors(),
                Map.of(searchJob.result().query().id(), searchJob.result()),
                executionInfo);
    }

}
