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
package org.graylog.plugins.views.search.engine;

import com.google.common.util.concurrent.Uninterruptibles;
import org.graylog.plugins.views.search.ParameterProvider;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchDomain;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.db.SearchJobService;
import org.graylog.plugins.views.search.engine.normalization.SearchNormalization;
import org.graylog.plugins.views.search.engine.validation.SearchValidation;
import org.graylog.plugins.views.search.errors.SearchError;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.ExecutionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SearchExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(SearchExecutor.class);

    private final SearchDomain searchDomain;
    private final SearchJobService searchJobService;
    private final QueryEngine queryEngine;
    private final SearchValidation searchValidation;
    private final SearchNormalization searchNormalization;

    @Inject
    public SearchExecutor(SearchDomain searchDomain,
                          SearchJobService searchJobService,
                          QueryEngine queryEngine,
                          SearchValidation searchValidation,
                          SearchNormalization searchNormalization) {
        this.searchDomain = searchDomain;
        this.searchJobService = searchJobService;
        this.queryEngine = queryEngine;
        this.searchValidation = searchValidation;
        this.searchNormalization = searchNormalization;
    }

    public SearchJob execute(String searchId, SearchUser searchUser, ExecutionState executionState) {
        return searchDomain.getForUser(searchId, searchUser)
                .map(s -> execute(s, searchUser, executionState))
                .orElseThrow(() -> new NotFoundException("No search found with id <" + searchId + ">."));
    }

    @Deprecated
    public Set<String> getFieldsPresentInSearchResultDocuments(final Search search, SearchUser searchUser, final int size) {
        final ExecutionState executionState = ExecutionState.empty();
        final Search preValidationSearch = searchNormalization.preValidation(search, searchUser, executionState);
        final Set<SearchError> validationErrors = searchValidation.validate(preValidationSearch, searchUser);
        if (hasFatalError(validationErrors)) {
            return Set.of();
        }
        final Search normalizedSearch = searchNormalization.postValidation(preValidationSearch, searchUser, executionState);
        final Query mainQuery = normalizedSearch.queries().stream().findFirst().orElseThrow(() -> new IllegalArgumentException("No queries in search : " + normalizedSearch.id()));
        return queryEngine.getFieldsPresentInSearchResultDocuments(mainQuery, size);
    }

    public Set<String> getFieldsPresentInSearchResultDocuments(final Query query,
                                                               final ParameterProvider parameterProvider,
                                                               final SearchUser searchUser,
                                                               final int size) {
        final ExecutionState executionState = ExecutionState.empty();
        final Query preValidationQuery = searchNormalization.preValidation(query, parameterProvider, searchUser, executionState);
        final Set<SearchError> validationErrors = searchValidation.validate(preValidationQuery, searchUser);
        if (hasFatalError(validationErrors)) {
            return Set.of();
        }
        final Query normalizedQuery = searchNormalization.postValidation(preValidationQuery, parameterProvider);
        return queryEngine.getFieldsPresentInSearchResultDocuments(normalizedQuery, size);
    }

    public SearchJob execute(Search search, SearchUser searchUser, ExecutionState executionState) {
        final Search preValidationSearch = searchNormalization.preValidation(search, searchUser, executionState);
        final Set<SearchError> validationErrors = searchValidation.validate(preValidationSearch, searchUser);
        if (hasFatalError(validationErrors)) {
            return searchJobWithFatalError(searchJobService.create(preValidationSearch, searchUser.username()), validationErrors);
        }
        final Search normalizedSearch = searchNormalization.postValidation(preValidationSearch, searchUser, executionState);

        final SearchJob searchJob = queryEngine.execute(searchJobService.create(normalizedSearch, searchUser.username()), validationErrors);
        validationErrors.forEach(searchJob::addError);

        try {
            Uninterruptibles.getUninterruptibly(searchJob.getResultFuture(), 60000, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            LOG.error("Error executing search job <{}>", searchJob.getId(), e);
            throw new InternalServerErrorException("Error executing search job: " + e.getMessage(), e);
        } catch (TimeoutException e) {
            throw new InternalServerErrorException("Timeout while executing search job");
        } catch (Exception e) {
            LOG.error("Other error", e);
            throw e;
        }

        return searchJob;
    }

    private SearchJob searchJobWithFatalError(SearchJob searchJob, Set<SearchError> validationErrors) {
        validationErrors.forEach(searchJob::addError);

        return searchJob;
    }

    private boolean hasFatalError(Set<SearchError> validationErrors) {
        return validationErrors.stream().anyMatch(SearchError::fatal);
    }
}
