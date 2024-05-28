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
import org.graylog.plugins.views.search.errors.SearchError;

import java.util.Map;
import java.util.Set;

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

}
