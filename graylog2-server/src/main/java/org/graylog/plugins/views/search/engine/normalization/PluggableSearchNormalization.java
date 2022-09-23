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
package org.graylog.plugins.views.search.engine.normalization;

import org.graylog.plugins.views.search.ParameterProvider;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.ExecutionState;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Set;

import static com.google.common.base.MoreObjects.firstNonNull;

public class PluggableSearchNormalization implements SearchNormalization {
    private final Set<SearchNormalizer> pluggableNormalizers;
    private final Set<SearchNormalizer> postValidationNormalizers;

    @Inject
    public PluggableSearchNormalization(Set<SearchNormalizer> pluggableNormalizers,
                                        @PostValidation Set<SearchNormalizer> postValidationNormalizers) {
        this.pluggableNormalizers = pluggableNormalizers;
        this.postValidationNormalizers = postValidationNormalizers;
    }

    public PluggableSearchNormalization(Set<SearchNormalizer> pluggableNormalizers) {
        this(pluggableNormalizers, Collections.emptySet());
    }

    private Search normalize(Search search, Set<SearchNormalizer> normalizers) {
        Search normalizedSearch = search;
        for (SearchNormalizer searchNormalizer : normalizers) {
            normalizedSearch = searchNormalizer.normalize(normalizedSearch);
        }

        return normalizedSearch;
    }

    private Query normalize(final Query query,
                            final ParameterProvider parameterProvider,
                            final Set<SearchNormalizer> normalizers) {
        Query normalizedQuery = query;
        for (SearchNormalizer searchNormalizer : normalizers) {
            normalizedQuery = searchNormalizer.normalizeQuery(normalizedQuery, parameterProvider);
        }
        return normalizedQuery;
    }

    @Override
    public Search preValidation(Search search, SearchUser searchUser, ExecutionState executionState) {
        final Search searchWithStreams = search.addStreamsToQueriesWithoutStreams(() -> searchUser.streams().loadAll());
        Search normalizedSearch = searchWithStreams.applyExecutionState(firstNonNull(executionState, ExecutionState.empty()));

        return normalize(normalizedSearch, pluggableNormalizers);
    }

    @Override
    public Search postValidation(Search search, SearchUser searchUser, ExecutionState executionState) {
        return normalize(search, postValidationNormalizers);
    }

    @Override
    public Query preValidation(final Query query, final ParameterProvider parameterProvider, SearchUser searchUser, ExecutionState executionState) {
        Query normalizedQuery = query;
        if (!query.hasStreams()) {
            normalizedQuery = query.addStreamsToFilter(searchUser.streams().loadAll());
        }

        if (!executionState.equals(ExecutionState.empty())) {
            normalizedQuery = normalizedQuery.applyExecutionState(executionState);
        }

        return normalize(normalizedQuery, parameterProvider, pluggableNormalizers);
    }

    @Override
    public Query postValidation(final Query query, final ParameterProvider parameterProvider) {
        return normalize(query, parameterProvider, postValidationNormalizers);
    }
}
