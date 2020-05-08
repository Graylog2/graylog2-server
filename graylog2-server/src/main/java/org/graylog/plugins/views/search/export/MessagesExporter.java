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

import com.google.common.eventbus.EventBus;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.events.MessagesExportRequestedEvent;
import org.graylog.plugins.views.search.events.MessagesExportSucceededEvent;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.inject.Inject;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class MessagesExporter {
    private final ExportBackend backend;
    private final ChunkDecorator chunkDecorator;
    private final CommandFactory commandFactory;
    @SuppressWarnings("UnstableApiUsage")
    private final EventBus eventBus;

    public Supplier<DateTime> startedAt = () -> DateTime.now(DateTimeZone.UTC);
    public Supplier<DateTime> finishedAt = () -> DateTime.now(DateTimeZone.UTC);

    @Inject
    public MessagesExporter(
            ExportBackend backend,
            ChunkDecorator chunkDecorator,
            CommandFactory commandFactory,
            @SuppressWarnings("UnstableApiUsage") EventBus eventBus) {
        this.backend = backend;
        this.chunkDecorator = chunkDecorator;
        this.commandFactory = commandFactory;
        this.eventBus = eventBus;
    }

    public void export(MessagesRequest request, String userName, Consumer<SimpleMessageChunk> chunkForwarder) {
        ExportMessagesCommand command = commandFactory.buildFromRequest(request);

        export(command, userName, chunkForwarder);
    }

    private void export(ExportMessagesCommand command, String userName, Consumer<SimpleMessageChunk> chunkForwarder) {
        post(MessagesExportRequestedEvent.from(startedAt.get(), userName, command));

        Consumer<SimpleMessageChunk> decoratedForwarder = chunk -> decorate(chunkForwarder, chunk, command);

        backend.run(command, decoratedForwarder);

        post(MessagesExportSucceededEvent.from(finishedAt.get(), userName, command));
    }

    private void post(Object event) {
        //noinspection UnstableApiUsage
        eventBus.post(requireNonNull(event));
    }

    public void export(Search search, ResultFormat resultFormat, String userName, Consumer<SimpleMessageChunk> chunkForwarder) {
        Query query = queryFrom(search);

        ExportMessagesCommand command = commandFactory.buildWithSearchOnly(search, query, resultFormat);

        export(command, userName, chunkForwarder);
    }

    public void export(Search search, String searchTypeId, ResultFormat resultFormat, String userName, Consumer<SimpleMessageChunk> chunkForwarder) {
        Query query = search.queryForSearchType(searchTypeId);

        MessageList messageList = messageListFrom(query, searchTypeId);

        ExportMessagesCommand command = commandFactory.buildWithMessageList(search, query, messageList, resultFormat);

        export(command, userName, chunkForwarder);
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

    private void decorate(Consumer<SimpleMessageChunk> chunkForwarder, SimpleMessageChunk chunk, ExportMessagesCommand command) {
        SimpleMessageChunk decoratedChunk = chunkDecorator.decorate(chunk, command);

        chunkForwarder.accept(decoratedChunk);
    }
}
