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
package org.graylog.plugins.views.search.rest.scriptingapi;

import com.google.common.eventbus.EventBus;
import jakarta.inject.Inject;
import org.apache.shiro.subject.Subject;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.engine.SearchExecutor;
import org.graylog.plugins.views.search.events.SearchJobExecutionEvent;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.ExecutionState;
import org.graylog.plugins.views.search.rest.scriptingapi.mapping.AggregationTabularResponseCreator;
import org.graylog.plugins.views.search.rest.scriptingapi.mapping.MessagesTabularResponseCreator;
import org.graylog.plugins.views.search.rest.scriptingapi.mapping.QueryFailedException;
import org.graylog.plugins.views.search.rest.scriptingapi.mapping.SearchRequestSpecToSearchMapper;
import org.graylog.plugins.views.search.rest.scriptingapi.request.AggregationRequestSpec;
import org.graylog.plugins.views.search.rest.scriptingapi.request.MessagesRequestSpec;
import org.graylog.plugins.views.search.rest.scriptingapi.response.TabularResponse;
import org.graylog2.plugin.database.users.User;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;


public class ScriptingApiServiceImpl implements ScriptingApiService {
    private final SearchExecutor searchExecutor;
    private final SearchRequestSpecToSearchMapper searchCreator;
    private final MessagesTabularResponseCreator messagesTabularResponseCreator;
    private final AggregationTabularResponseCreator aggregationTabularResponseCreator;
    private final EventBus serverEventBus;

    @Inject
    public ScriptingApiServiceImpl(final SearchExecutor searchExecutor,
                                   final SearchRequestSpecToSearchMapper searchCreator,
                                   final MessagesTabularResponseCreator messagesTabularResponseCreator,
                                   final AggregationTabularResponseCreator aggregationTabularResponseCreator,
                                   final EventBus serverEventBus) {
        this.searchExecutor = searchExecutor;
        this.searchCreator = searchCreator;
        this.messagesTabularResponseCreator = messagesTabularResponseCreator;
        this.aggregationTabularResponseCreator = aggregationTabularResponseCreator;
        this.serverEventBus = serverEventBus;
    }

    public TabularResponse executeQuery(MessagesRequestSpec messagesRequestSpec, SearchUser searchUser, Subject subject) throws QueryFailedException {
        //Step 1: map simple request to more complex search
        Search search = searchCreator.mapToSearch(messagesRequestSpec, searchUser);

        //Step 2: execute search as we usually do
        final SearchJob searchJob = searchExecutor.executeSync(search, searchUser, ExecutionState.empty());
        postAuditEvent(searchJob, searchUser.getUser());

        //Step 3: take complex response and try to map it to simpler, tabular form
        return messagesTabularResponseCreator.mapToResponse(messagesRequestSpec, searchJob, searchUser, subject);
    }

    public TabularResponse executeAggregation(AggregationRequestSpec aggregationRequestSpec, SearchUser searchUser) throws QueryFailedException {
        //Step 1: map simple request to more complex search
        Search search = searchCreator.mapToSearch(aggregationRequestSpec, searchUser);

        //Step 2: execute search as we usually do
        final SearchJob searchJob = searchExecutor.executeSync(search, searchUser, ExecutionState.empty());
        postAuditEvent(searchJob, searchUser.getUser());

        //Step 3: take complex response and try to map it to simpler, tabular form
        return aggregationTabularResponseCreator.mapToResponse(aggregationRequestSpec, searchJob, searchUser);
    }

    private void postAuditEvent(SearchJob searchJob, User user) {
        final SearchJobExecutionEvent searchJobExecutionEvent = SearchJobExecutionEvent.create(user, searchJob, DateTime.now(DateTimeZone.UTC));
        this.serverEventBus.post(searchJobExecutionEvent);
    }
}
