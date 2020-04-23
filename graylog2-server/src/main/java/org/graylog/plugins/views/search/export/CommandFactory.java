package org.graylog.plugins.views.search.export;

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.searchtypes.MessageList;
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

    public MessagesRequest buildWithSearchOnly(Search search, Query query, ResultFormat resultFormat) {
        return builderFrom(resultFormat)
                .timeRange(query.timerange())
                .queryString(queryStringFrom(search, query))
                .streams(query.usedStreamIds())
                .build();
    }

    public MessagesRequest buildWithMessageList(Search search, Query query, MessageList messageList, ResultFormat resultFormat) {
        MessagesRequest.Builder requestBuilder = builderFrom(resultFormat)
                .timeRange(timeRangeFrom(query, messageList))
                .queryString(queryStringFrom(search, query, messageList))
                .streams(streamsFrom(query, messageList));

        if (messageList.sort() != null && resultFormat.sort().isEmpty()) {
            requestBuilder.sort(new LinkedHashSet<>(messageList.sort()));
        }

        return requestBuilder.build();
    }

    private MessagesRequest.Builder builderFrom(ResultFormat resultFormat) {
        MessagesRequest.Builder requestBuilder = MessagesRequest.builder();

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
