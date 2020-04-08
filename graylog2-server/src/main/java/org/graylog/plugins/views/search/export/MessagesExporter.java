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
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.searchtypes.MessageList;

import javax.inject.Inject;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class MessagesExporter {
    private final Defaults defaults;
    private final ExportBackend backend;

    @Inject
    public MessagesExporter(Defaults defaults, ExportBackend backend) {
        this.defaults = defaults;
        this.backend = backend;
    }

    public void export(MessagesRequest request, ChunkForwarder<SimpleMessageChunk> chunkForwarder) {
        MessagesRequest fullRequest = defaults.fillInIfNecessary(request);

        backend.run(fullRequest, chunkForwarder::write, chunkForwarder::close);
    }

    public void export(Search search, ResultFormat resultFormat, ChunkForwarder<SimpleMessageChunk> chunkForwarder) {
        exportWithRequestFrom(search, null, resultFormat, chunkForwarder);
    }

    public void export(Search search, String searchTypeId, ResultFormat resultFormat, ChunkForwarder<SimpleMessageChunk> chunkForwarder) {
        exportWithRequestFrom(search, searchTypeId, resultFormat, chunkForwarder);
    }

    private void exportWithRequestFrom(Search search, String searchTypeId, ResultFormat resultFormat, ChunkForwarder<SimpleMessageChunk> chunkForwarder) {
        MessagesRequest request = buildRequest(search, searchTypeId, resultFormat);

        export(request, chunkForwarder);
    }

    private MessagesRequest buildRequest(Search search, String searchTypeId, ResultFormat resultFormat) {
        Query query = singleQueryFrom(search);

        MessagesRequest.Builder requestBuilder = MessagesRequest.builder();

        setTimeRange(query, searchTypeId, requestBuilder);
        trySetQueryString(query, searchTypeId, requestBuilder);
        setStreams(query, searchTypeId, requestBuilder);
        trySetFields(resultFormat, requestBuilder);
        trySetSort(query, searchTypeId, resultFormat, requestBuilder);

        return requestBuilder.build();
    }

    private Query singleQueryFrom(Search s) {
        if (s.queries().size() > 1)
            throw new ExportException("Can't get messages for search with id" + s.id() + ", because it contains multiple queries");

        return s.queries().stream().findFirst()
                .orElseThrow(() -> new ExportException("Invalid Search object with empty Query"));
    }

    private void setTimeRange(Query query, String searchTypeId, MessagesRequest.Builder requestBuilder) {
        Optional<MessageList> ml = messageListFrom(query, searchTypeId);
        if (ml.isPresent() && ml.get().timerange().isPresent()) {
            requestBuilder.timeRange(query.effectiveTimeRange(ml.get()));
        } else {
            requestBuilder.timeRange(query.timerange());
        }
    }

    private void trySetQueryString(Query query, String searchTypeId, MessagesRequest.Builder requestBuilder) {
        Optional<MessageList> ml = messageListFrom(query, searchTypeId);
        boolean messageListHasQueryString = ml.isPresent() && ml.get().query().isPresent();
        boolean queryHasQueryString = query.query() instanceof ElasticsearchQueryString;

        if (messageListHasQueryString && queryHasQueryString) {
            requestBuilder.queryString(query.query());
            requestBuilder.additionalQueryString(ml.get().query().get());
        } else if (queryHasQueryString) {
            requestBuilder.queryString(query.query());
        } else if (messageListHasQueryString) {
            requestBuilder.queryString(ml.get().query().get());
        }
    }

    private void setStreams(Query query, String searchTypeId, MessagesRequest.Builder requestBuilder) {
        Optional<MessageList> messageList = messageListFrom(query, searchTypeId);
        if (messageList.isPresent()) {
            Set<String> streams = messageList.get().effectiveStreams().isEmpty() ?
                    query.usedStreamIds() :
                    messageList.get().effectiveStreams();
            requestBuilder.streams(streams);
        } else {
            requestBuilder.streams(query.usedStreamIds());
        }
    }

    private void trySetFields(ResultFormat resultFormat, MessagesRequest.Builder requestBuilder) {
        resultFormat.fieldsInOrder().ifPresent(requestBuilder::fieldsInOrder);
    }

    private void trySetSort(Query query, String searchTypeId, ResultFormat resultFormat, MessagesRequest.Builder requestBuilder) {
        Optional<MessageList> ml = messageListFrom(query, searchTypeId);
        if (resultFormat.sort().isPresent()) {
            requestBuilder.sort(resultFormat.sort().get());
        } else if (ml.isPresent() && ml.get().sort() != null) {
            requestBuilder.sort(new LinkedHashSet<>(ml.get().sort()));
        }
    }

    private Optional<MessageList> messageListFrom(Query query, String searchTypeId) {
        Optional<SearchType> searchType = searchTypeFrom(query, searchTypeId);

        if (!searchType.isPresent()) {
            return Optional.empty();
        }
        return searchType.map(st -> {
            if (!(st instanceof MessageList)) {
                throw new ExportException("Only message lists are currently supported");
            }
            return (MessageList) st;
        });
    }

    private Optional<SearchType> searchTypeFrom(Query query, String searchTypeId) {
        return query.searchTypes().stream()
                .filter(st -> st.id().equals(searchTypeId))
                .findFirst();
    }

    private String reduce(LinkedHashSet<LinkedHashSet<String>> hits) {
        return hits.stream()
                .map(row -> String.join(" ", row))
                .collect(Collectors.joining("\r\n"));
    }
}
