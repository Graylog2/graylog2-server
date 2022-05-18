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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.ExecutionState;

import javax.inject.Inject;
import java.util.Set;

import static com.google.common.base.MoreObjects.firstNonNull;

public class PluggableSearchNormalization implements SearchNormalization {
    private final ObjectMapper objectMapper;
    private final Set<SearchNormalizer> pluggableNormalizers;

    @Inject
    public PluggableSearchNormalization(ObjectMapper objectMapper, Set<SearchNormalizer> pluggableNormalizers) {
        this.objectMapper = objectMapper;
        this.pluggableNormalizers = pluggableNormalizers;
   }

    public Search normalize(Search search, SearchUser searchUser, ExecutionState executionState) {
        final Search searchWithStreams = search.addStreamsToQueriesWithoutStreams(() -> searchUser.streams().loadAll());

        Search normalizedSearch = searchWithStreams.applyExecutionState(objectMapper, firstNonNull(executionState, ExecutionState.empty()));

        for (SearchNormalizer searchNormalizer : pluggableNormalizers) {
            normalizedSearch = searchNormalizer.normalize(normalizedSearch, searchUser, executionState);
        }

        return normalizedSearch;
    }
}
