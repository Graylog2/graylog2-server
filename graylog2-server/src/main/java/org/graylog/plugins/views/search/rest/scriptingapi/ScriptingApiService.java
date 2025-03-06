package org.graylog.plugins.views.search.rest.scriptingapi;

import org.apache.shiro.subject.Subject;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.scriptingapi.mapping.QueryFailedException;
import org.graylog.plugins.views.search.rest.scriptingapi.request.AggregationRequestSpec;
import org.graylog.plugins.views.search.rest.scriptingapi.request.MessagesRequestSpec;
import org.graylog.plugins.views.search.rest.scriptingapi.response.TabularResponse;

public interface ScriptingApiService {
    TabularResponse executeQuery(MessagesRequestSpec messagesRequestSpec, SearchUser searchUser, Subject subject) throws QueryFailedException;
    TabularResponse executeAggregation(AggregationRequestSpec aggregationRequestSpec, SearchUser searchUser) throws QueryFailedException;
}
