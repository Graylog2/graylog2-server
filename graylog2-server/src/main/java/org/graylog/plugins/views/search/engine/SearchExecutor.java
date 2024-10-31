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
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.inject.Inject;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import org.graylog.plugins.views.search.ExplainResults;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchDomain;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.db.SearchJobService;
import org.graylog.plugins.views.search.engine.normalization.SearchNormalization;
import org.graylog.plugins.views.search.engine.validation.SearchValidation;
import org.graylog.plugins.views.search.errors.SearchError;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.ExecutionState;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SearchExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(SearchExecutor.class);
    private static final DateTimeZone DEFAULT_TIMEZONE = DateTimeZone.UTC;

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

    public SearchJob executeSync(String searchId, SearchUser searchUser, ExecutionState executionState) {
        return searchDomain.getForUser(searchId, searchUser)
                .map(s -> executeSync(s, searchUser, executionState))
                .orElseThrow(() -> new NotFoundException("No search found with id <" + searchId + ">."));
    }

    public SearchJob executeAsync(String searchId, SearchUser searchUser, ExecutionState executionState) {
        return searchDomain.getForUser(searchId, searchUser)
                .map(s -> executeAsync(s, searchUser, executionState))
                .orElseThrow(() -> new NotFoundException("No search found with id <" + searchId + ">."));
    }

    @WithSpan
    public SearchJob executeSync(Search search, SearchUser searchUser, ExecutionState executionState) {
        final SearchJob searchJob = prepareAndExecuteSearchJob(search, searchUser, executionState);

        try {
            final CompletableFuture<Void> resultFuture = searchJob.getResultFuture();
            /* TODO
             * Lines  103-104 mimic the previous behavior (the join was in QueryEngine, that's the difference)
             * It also shows some problems in the code, where parts of the code expressed the necessity to wait indefinitely for the results (joins),
             * while parts of the code expressed the need to use timeout (see line 104).
             * Imho in a separate PR we should decide which way to go, currently we wait indefinitely, as we did so far, and line 104 probably does not have a lot of sense, as it did not have before.
             */
            if (resultFuture != null) {
                resultFuture.join();
                Uninterruptibles.getUninterruptibly(resultFuture, 60000, TimeUnit.MILLISECONDS);
            }
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

    @WithSpan
    public SearchJob executeAsync(Search search, SearchUser searchUser, ExecutionState executionState) {
        return prepareAndExecuteSearchJob(search, searchUser, executionState);
    }

    private SearchJob prepareAndExecuteSearchJob(final Search search,
                                                 final SearchUser searchUser,
                                                 final ExecutionState executionState) {
        final Search preValidationSearch = searchNormalization.preValidation(search, searchUser, executionState);

        final Set<SearchError> validationErrors = searchValidation.validate(preValidationSearch, searchUser);

        if (hasFatalError(validationErrors)) {
            return searchJobWithFatalError(searchJobService.create(preValidationSearch, searchUser.username(), executionState.cancelAfterSeconds()), validationErrors);
        }

        final Search normalizedSearch = searchNormalization.postValidation(preValidationSearch, searchUser, executionState);
        final SearchJob searchJob = queryEngine.execute(searchJobService.create(normalizedSearch, searchUser.username(), executionState.cancelAfterSeconds()), validationErrors, searchUser.timeZone().orElse(DEFAULT_TIMEZONE));
        validationErrors.forEach(searchJob::addError);
        return searchJob;
    }

    public ExplainResults explain(String searchId, SearchUser searchUser, ExecutionState executionState) {
        return searchDomain.getForUser(searchId, searchUser)
                .map(s -> explain(s, searchUser, executionState))
                .orElseThrow(() -> new NotFoundException("No search found with id <" + searchId + ">."));
    }

    public ExplainResults explain(Search search, SearchUser searchUser, ExecutionState executionState) {
        final Search preValidationSearch = searchNormalization.preValidation(search, searchUser, executionState);
        final Set<SearchError> validationErrors = searchValidation.validate(preValidationSearch, searchUser);
        final Search normalizedSearch = searchNormalization.postValidation(preValidationSearch, searchUser, executionState);

        return queryEngine.explain(searchJobService.create(normalizedSearch, searchUser.username(), executionState.cancelAfterSeconds()), validationErrors,
                searchUser.timeZone().orElse(DEFAULT_TIMEZONE));
    }

    private SearchJob searchJobWithFatalError(SearchJob searchJob, Set<SearchError> validationErrors) {
        validationErrors.forEach(searchJob::addError);

        return searchJob;
    }

    private boolean hasFatalError(Set<SearchError> validationErrors) {
        return validationErrors.stream().anyMatch(SearchError::fatal);
    }
}
