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
import org.graylog.plugins.views.search.searchtypes.MessageList;

import javax.inject.Inject;
import java.util.function.Consumer;

public class MessagesExporter {
    private final ExportBackend backend;
    private final ChunkDecorator chunkDecorator;
    private final CommandFactory commandFactory;

    @Inject
    public MessagesExporter(ExportBackend backend, ChunkDecorator chunkDecorator, CommandFactory commandFactory) {
        this.backend = backend;
        this.chunkDecorator = chunkDecorator;
        this.commandFactory = commandFactory;
    }

    public void export(MessagesRequest request, Consumer<SimpleMessageChunk> chunkForwarder) {
        backend.run(request, chunkForwarder);
    }

    public void export(Search search, ResultFormat resultFormat, Consumer<SimpleMessageChunk> chunkForwarder) {
        Query query = queryFrom(search);

        MessagesRequest request = commandFactory.buildWithSearchOnly(search, query, resultFormat);

        export(request, chunkForwarder);
    }

    public void export(Search search, String searchTypeId, ResultFormat resultFormat, Consumer<SimpleMessageChunk> chunkForwarder) {
        Query query = search.queryForSearchType(searchTypeId);

        MessageList messageList = messageListFrom(query, searchTypeId);

        MessagesRequest request = commandFactory.buildWithMessageList(search, query, messageList, resultFormat);

        Consumer<SimpleMessageChunk> decoratedForwarder = chunk -> decorate(chunkForwarder, messageList, chunk, request);

        export(request, decoratedForwarder);
    }

    private MessageList messageListFrom(Query query, String searchTypeId) {

        SearchType searchType = query.searchTypes().stream()
                .filter(st -> st.id().equals(searchTypeId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Error getting search type"));

        if (!(searchType instanceof MessageList)) {
            throw new ExportException("export is not supported for search type " + searchType.getClass());
        }
        return (MessageList) searchType;
    }

    private Query queryFrom(Search s) {
        if (s.queries().size() > 1) {
            throw new ExportException("Can't get messages for search with id " + s.id() + ", because it contains multiple queries");
        }

        return s.queries().stream().findFirst()
                .orElseThrow(() -> new ExportException("Invalid Search object with empty Query"));
    }

    private void decorate(Consumer<SimpleMessageChunk> chunkForwarder, MessageList messageList, SimpleMessageChunk chunk, MessagesRequest request) {
        SimpleMessageChunk decoratedChunk = chunkDecorator.decorate(chunk, messageList.decorators(), request);

        chunkForwarder.accept(decoratedChunk);
    }
}
