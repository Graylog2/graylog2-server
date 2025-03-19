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
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchJobIdentifier;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.errors.SearchError;
import org.graylog.plugins.views.search.jobs.SearchJobState;
import org.graylog.plugins.views.search.jobs.SearchJobStatus;
import org.graylog.plugins.views.search.searchtypes.results.PaginableResults;

import java.util.Map;
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
        @JsonProperty ExecutionInfo execution,
        @JsonProperty("progress") int progress) {

    public SearchJobDTO withResultsLimitedTo(final int page, final int perPage) {
        if (page == 0 || perPage == 0 || !hasOnlyOnePaginableSearchType()) {
            return this;
        } else {
            final QueryResult queryResultLimited = results().values().stream()
                    .findFirst()
                    .map(queryResult -> {
                                final PaginableResults<?> paginableResults = (PaginableResults<?>) queryResult
                                        .searchTypes()
                                        .values()
                                        .stream()
                                        .findFirst()
                                        .get();
                                return queryResult.toBuilder()
                                        .searchTypes(Map.of(paginableResults.id(), paginableResults.withResultsLimitedTo(page, perPage)))
                                        .build();
                            }
                    )
                    .get();
            return new SearchJobDTO(searchJobIdentifier(),
                    errors(),
                    Map.of(queryResultLimited.query().id(), queryResultLimited),
                    execution(),
                    progress);
        }
    }

    private boolean hasOnlyOnePaginableSearchType() {
        if (results.size() != 1) {
            return false;
        }

        final Map<String, SearchType.Result> resultsOfTheOnlyQuery = results().values().iterator().next().searchTypes();
        return resultsOfTheOnlyQuery.size() == 1 &&
                resultsOfTheOnlyQuery.values().stream().findFirst().get() instanceof PaginableResults<?>;
    }


    public static SearchJobDTO fromSearchJob(final SearchJob searchJob) {
        final ExecutionInfo executionInfo = searchJob.execution();
        return new SearchJobDTO(
                searchJob.getSearchJobIdentifier(),
                searchJob.getErrors(),
                searchJob.results(),
                executionInfo,
                0);
    }

    public static SearchJobDTO fromSearchJob(final SearchJob searchJob,
                                             final int progress,
                                             final QueryResult dbResults) {
        final ExecutionInfo executionInfo = searchJob.execution();
        final Map<String, QueryResult> inMemoryResults = searchJob.results();
        return new SearchJobDTO(
                searchJob.getSearchJobIdentifier(),
                searchJob.getErrors(),
                inMemoryResults.isEmpty() ? Map.of(dbResults.query().id(), dbResults) : inMemoryResults,
                executionInfo,
                progress);
    }

    public static SearchJobDTO fromSearchJobState(final SearchJobState searchJob) {
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
        final boolean hasQuery = searchJob.result() != null
                && searchJob.result().query() != null
                && searchJob.result().query().id() != null;
        return new SearchJobDTO(
                searchJob.identifier(),
                searchJob.errors(),
                hasQuery ? Map.of(searchJob.result().query().id(), searchJob.result()) : Map.of(),
                executionInfo,
                searchJob.progress());
    }

}
