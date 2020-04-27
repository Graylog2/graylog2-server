/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.views.search.export;

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.inject.Inject;
import java.util.LinkedHashSet;
import java.util.Set;

public class CommandFactory {
    private final QueryStringDecorator queryStringDecorator;

    @Inject
    public CommandFactory(QueryStringDecorator queryStringDecorator) {
        this.queryStringDecorator = queryStringDecorator;
    }

    public ExportMessagesCommand buildFromRequest(MessagesRequest request) {
        ExportMessagesCommand.Builder builder = ExportMessagesCommand.builder()
                .timeRange(toAbsolute(request.timeRange()))
                .queryString(request.queryString())
                .streams(request.streams())
                .fieldsInOrder(request.fieldsInOrder())
                .sort(request.sort());

        if (request.limit().isPresent()) {
            builder.limit(request.limit().getAsInt());
        }

        return builder.build();
    }

    public ExportMessagesCommand buildWithSearchOnly(Search search, Query query, ResultFormat resultFormat) {
        return builderFrom(resultFormat)
                .timeRange(toAbsolute(query.timerange()))
                .queryString(queryStringFrom(search, query))
                .streams(query.usedStreamIds())
                .build();
    }

    public ExportMessagesCommand buildWithMessageList(Search search, Query query, MessageList messageList, ResultFormat resultFormat) {
        ExportMessagesCommand.Builder commandBuilder = builderFrom(resultFormat)
                .timeRange(toAbsolute(timeRangeFrom(query, messageList)))
                .queryString(queryStringFrom(search, query, messageList))
                .streams(streamsFrom(query, messageList))
                .decorators(messageList.decorators());

        if (messageList.sort() != null && resultFormat.sort().isEmpty()) {
            commandBuilder.sort(new LinkedHashSet<>(messageList.sort()));
        }

        return commandBuilder.build();
    }

    private AbsoluteRange toAbsolute(TimeRange timeRange) {
        return AbsoluteRange.create(timeRange.getFrom(), timeRange.getTo());
    }

    private ExportMessagesCommand.Builder builderFrom(ResultFormat resultFormat) {
        ExportMessagesCommand.Builder requestBuilder = ExportMessagesCommand.builder();

        requestBuilder.fieldsInOrder(resultFormat.fieldsInOrder());

        if (!resultFormat.sort().isEmpty()) {
            requestBuilder.sort(resultFormat.sort());
        }

        if (resultFormat.limit().isPresent()) {
            requestBuilder.limit(resultFormat.limit().getAsInt());
        }

        return requestBuilder;
    }

    private TimeRange timeRangeFrom(Query query, MessageList ml) {
        if (ml.timerange().isPresent()) {
            return query.effectiveTimeRange(ml);
        } else {
            return query.timerange();
        }
    }

    private ElasticsearchQueryString queryStringFrom(Search search, Query query) {
        ElasticsearchQueryString undecorated = queryStringFrom(query);
        return decorateQueryString(search, query, undecorated);
    }

    private ElasticsearchQueryString queryStringFrom(Search search, Query query, MessageList messageList) {
        ElasticsearchQueryString undecorated = pickQueryString(messageList, query);
        return decorateQueryString(search, query, undecorated);
    }

    private ElasticsearchQueryString pickQueryString(MessageList messageList, Query query) {
        if (messageList.query().isPresent() && hasQueryString(query)) {
            return esQueryStringFrom(query).concatenate(esQueryStringFrom(messageList));
        } else if (messageList.query().isPresent()) {
            return esQueryStringFrom(messageList);
        } else {
            return queryStringFrom(query);
        }
    }

    private boolean hasQueryString(Query query) {
        return query.query() instanceof ElasticsearchQueryString;
    }

    private ElasticsearchQueryString queryStringFrom(Query query) {
        return hasQueryString(query) ? esQueryStringFrom(query) : ElasticsearchQueryString.empty();
    }

    private ElasticsearchQueryString esQueryStringFrom(MessageList ml) {
        //noinspection OptionalGetWithoutIsPresent
        return (ElasticsearchQueryString) ml.query().get();
    }

    private ElasticsearchQueryString esQueryStringFrom(Query query) {
        return (ElasticsearchQueryString) query.query();
    }

    private ElasticsearchQueryString decorateQueryString(Search search, Query query, ElasticsearchQueryString undecorated) {
        String queryString = undecorated.queryString();
        String decorated = queryStringDecorator.decorateQueryString(queryString, search, query);
        return ElasticsearchQueryString.builder().queryString(decorated).build();
    }

    private Set<String> streamsFrom(Query query, MessageList messageList) {
        return messageList.effectiveStreams().isEmpty() ?
                query.usedStreamIds() :
                messageList.effectiveStreams();
    }
}
