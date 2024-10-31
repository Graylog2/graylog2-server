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

import jakarta.inject.Inject;
import org.graylog.plugins.views.search.ParameterProvider;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.ExecutionState;
import org.graylog.plugins.views.search.rest.ExecutionStateGlobalOverride;
import org.graylog2.plugin.Tools;
import org.graylog2.streams.StreamService;
import org.joda.time.DateTime;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.google.common.base.MoreObjects.firstNonNull;

public class PluggableSearchNormalization implements SearchNormalization {
    private final Set<SearchNormalizer> pluggableNormalizers;
    private final Set<SearchNormalizer> postValidationNormalizers;
    private final Function<Collection<String>, Stream<String>> streamCategoryMapper;

    @Inject
    public PluggableSearchNormalization(Set<SearchNormalizer> pluggableNormalizers,
                                        @PostValidation Set<SearchNormalizer> postValidationNormalizers,
                                        StreamService streamService) {
        this.pluggableNormalizers = pluggableNormalizers;
        this.postValidationNormalizers = postValidationNormalizers;
        this.streamCategoryMapper = (categories) -> streamService.mapCategoriesToIds(categories).stream();
    }

    public PluggableSearchNormalization(Set<SearchNormalizer> pluggableNormalizers, StreamService streamService) {
        this(pluggableNormalizers, Collections.emptySet(), streamService);
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
        final Search searchWithStreams = search
                .addStreamsToQueriesWithCategories(streamCategoryMapper, searchUser)
                .addStreamsToSearchTypesWithCategories(streamCategoryMapper, searchUser)
                .addStreamsToQueriesWithoutStreams(() -> searchUser.streams().loadMessageStreamsWithFallback());
        final var now = referenceDateFromOverrideOrNow(executionState);
        final var normalizedSearch = searchWithStreams.applyExecutionState(firstNonNull(executionState, ExecutionState.empty()))
                .withReferenceDate(now);

        return normalize(normalizedSearch, pluggableNormalizers);
    }

    private DateTime referenceDateFromOverrideOrNow(ExecutionState executionState) {
        return Optional.ofNullable(executionState)
                .map(ExecutionState::globalOverride)
                .flatMap(ExecutionStateGlobalOverride::now)
                .orElse(Tools.nowUTC());
    }

    @Override
    public Search postValidation(Search search, SearchUser searchUser, ExecutionState executionState) {
        return normalize(search, postValidationNormalizers);
    }

    @Override
    public Query preValidation(final Query query, final ParameterProvider parameterProvider, SearchUser searchUser, ExecutionState executionState) {
        Query normalizedQuery = query;
        if (!query.hasStreams()) {
            normalizedQuery = query.addStreamsToFilter(searchUser.streams().loadMessageStreamsWithFallback());
        } else if (!query.usedStreamCategories().isEmpty()) {
            normalizedQuery = query.replaceStreamCategoryFilters(streamCategoryMapper, searchUser);
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
