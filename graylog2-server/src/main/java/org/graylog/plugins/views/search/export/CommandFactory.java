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
package org.graylog.plugins.views.search.export;

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog2.decorators.Decorator;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
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
                .chunkSize(request.chunkSize());

        if (request.limit().isPresent()) {
            builder.limit(request.limit().getAsInt());
        }

        return builder.build();
    }

    public ExportMessagesCommand buildWithSearchOnly(Search search, ResultFormat resultFormat) {
        Query query = queryFrom(search);

        return builderFrom(resultFormat)
                .timeRange(resultFormat.timerange().orElse(toAbsolute(query.timerange())))
                .queryString(queryStringFrom(search, query))
                .streams(query.usedStreamIds())
                .build();
    }

    private Query queryFrom(Search s) {
        if (s.queries().size() > 1) {
            throw new ExportException("Can't get messages for search with id " + s.id() + ", because it contains multiple queries");
        }

        return s.queries().stream().findFirst()
                .orElseThrow(() -> new ExportException("Invalid Search object with empty Query"));
    }

    public ExportMessagesCommand buildWithMessageList(Search search, String messageListId, ResultFormat resultFormat) {
        Query query = search.queryForSearchType(messageListId);
        SearchType searchType = searchTypeFrom(query, messageListId);

        final List<Decorator> decorators = searchType instanceof MessageList ? ((MessageList) searchType).decorators() : Collections.emptyList();

        ExportMessagesCommand.Builder commandBuilder = builderFrom(resultFormat)
                .timeRange(resultFormat.timerange().orElse(toAbsolute(timeRangeFrom(query, searchType))))
                .queryString(queryStringFrom(search, query, searchType))
                .streams(streamsFrom(query, searchType))
                .decorators(decorators);

        return commandBuilder.build();
    }

    private SearchType searchTypeFrom(Query query, String searchTypeId) {
        SearchType searchType = query.searchTypes().stream()
                .filter(st -> st.id().equals(searchTypeId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Error getting search type"));

        if (!searchType.isExportable()) {
            throw new ExportException("export is not supported for search type " + searchType.getClass());
        }
        return searchType;
    }

    private AbsoluteRange toAbsolute(TimeRange timeRange) {
        return AbsoluteRange.create(timeRange.getFrom(), timeRange.getTo());
    }

    private ExportMessagesCommand.Builder builderFrom(ResultFormat resultFormat) {
        ExportMessagesCommand.Builder requestBuilder = ExportMessagesCommand.builder();

        requestBuilder.fieldsInOrder(resultFormat.fieldsInOrder());

        if (resultFormat.limit().isPresent()) {
            requestBuilder.limit(resultFormat.limit().orElseThrow(() -> new IllegalStateException("No value present!")));
        }

        return requestBuilder;
    }

    private TimeRange timeRangeFrom(Query query, SearchType searchType) {
        if (searchType.timerange().isPresent()) {
            return query.effectiveTimeRange(searchType);
        } else {
            return query.timerange();
        }
    }

    private ElasticsearchQueryString queryStringFrom(Search search, Query query) {
        ElasticsearchQueryString undecorated = queryStringFrom(query);
        return decorateQueryString(search, query, undecorated);
    }

    private ElasticsearchQueryString queryStringFrom(Search search, Query query, SearchType searchType) {
        ElasticsearchQueryString undecorated = pickQueryString(searchType, query);
        return decorateQueryString(search, query, undecorated);
    }

    private ElasticsearchQueryString pickQueryString(SearchType searchType, Query query) {
        if (searchType.query().isPresent() && hasQueryString(query)) {
            return esQueryStringFrom(query).concatenate(esQueryStringFrom(searchType));
        } else if (searchType.query().isPresent()) {
            return esQueryStringFrom(searchType);
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

    private ElasticsearchQueryString esQueryStringFrom(SearchType searchType) {
        //noinspection OptionalGetWithoutIsPresent
        return (ElasticsearchQueryString) searchType.query().get();
    }

    private ElasticsearchQueryString esQueryStringFrom(Query query) {
        return (ElasticsearchQueryString) query.query();
    }

    private ElasticsearchQueryString decorateQueryString(Search search, Query query, ElasticsearchQueryString undecorated) {
        String queryString = undecorated.queryString();
        String decorated = queryStringDecorator.decorateQueryString(queryString, search, query);
        return ElasticsearchQueryString.builder().queryString(decorated).build();
    }

    private Set<String> streamsFrom(Query query, SearchType searchType) {
        return searchType.effectiveStreams().isEmpty() ?
                query.usedStreamIds() :
                searchType.effectiveStreams();
    }
}
