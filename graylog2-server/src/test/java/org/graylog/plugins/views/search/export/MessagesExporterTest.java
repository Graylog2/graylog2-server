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

import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.events.MessagesExportEvent;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.function.Consumer;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.graylog.plugins.views.search.TestData.validQueryBuilder;
import static org.graylog.plugins.views.search.export.LinkedHashSetUtil.linkedHashSetOf;
import static org.graylog.plugins.views.search.export.TestData.validQueryBuilderWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessagesExporterTest {

    private ExportBackend backend;
    private MessagesExporter sut;
    private ChunkDecorator chunkDecorator;
    private CommandFactory commandFactory;
    @SuppressWarnings("UnstableApiUsage")
    private EventBus eventBus;

    @BeforeEach
    void setUp() {
        backend = mock(ExportBackend.class);
        chunkDecorator = mock(ChunkDecorator.class);
        commandFactory = mock(CommandFactory.class);
        when(commandFactory.buildFromRequest(any())).thenReturn(ExportMessagesCommand.withDefaults());
        when(commandFactory.buildWithSearchOnly(any(), any(), any())).thenReturn(ExportMessagesCommand.withDefaults());
        when(commandFactory.buildWithMessageList(any(), any(), any(), any())).thenReturn(ExportMessagesCommand.withDefaults());
        //noinspection UnstableApiUsage
        eventBus = mock(EventBus.class);
        sut = new MessagesExporter(backend, chunkDecorator, commandFactory, eventBus);
    }

    @Test
    void throwsIfSearchTypeIsNotMessageList() {
        Pivot p = Pivot.builder().id("pivot-id").series(newArrayList()).rollup(false).build();

        Query q = validQueryBuilder().searchTypes(ImmutableSet.of(p)).build();

        Search s = searchWithQueries(q);

        assertThatExceptionOfType(ExportException.class)
                .isThrownBy(() -> exportSearchType(s, p.id(), ResultFormat.builder().build()))
                .withMessageContaining("supported");
    }

    @Test
    void searchWithMultipleQueriesLeadsToExceptionIfNoSearchTypeProvided() {
        Search s = searchWithQueries(validQueryBuilder().build(), validQueryBuilder().build());

        assertThatExceptionOfType(ExportException.class)
                .isThrownBy(() -> exportSearch(s, ResultFormat.builder().build()))
                .withMessageContaining("multiple queries");
    }

    @Test
    void picksCorrectQueryIfSearchTypeProvided() {
        MessageList ml = MessageList.builder().id("ml-id").build();
        Query correctQuery = validQueryBuilderWith(ml).timerange(timeRange(222)).build();
        Query otherQuery = validQueryBuilder().build();

        Search s = searchWithQueries(correctQuery, otherQuery);

        ResultFormat resultFormat = ResultFormat.builder().build();
        exportSearchType(s, ml.id(), resultFormat);

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(commandFactory).buildWithMessageList(eq(s), captor.capture(), eq(ml), eq(resultFormat));

        assertThat(captor.getValue()).isEqualTo(correctQuery);
    }

    @Test
    void appliesDecorators() {
        MessageList messageList = MessageList.builder().id("ml-id").build();
        Search search = searchWithQueries(validQueryBuilderWith(messageList).build());

        SimpleMessageChunk undecoratedChunk = SimpleMessageChunk.from(linkedHashSetOf("field-1"), linkedHashSetOf());
        SimpleMessageChunk decoratedChunk = SimpleMessageChunk.from(linkedHashSetOf("field-1", "field-2"), linkedHashSetOf());

        when(chunkDecorator.decorate(eq(undecoratedChunk), any())).thenReturn(decoratedChunk);

        ArrayList<SimpleMessageChunk> results = exportSearchTypeWithStubbedSingleChunkFromBackend(search, messageList.id(), undecoratedChunk);

        assertThat(results).containsExactly(decoratedChunk);
    }

    @Test
    void sendsAuditEvent() {
        ExportMessagesCommand command = ExportMessagesCommand.withDefaults();
        when(commandFactory.buildFromRequest(any())).
                thenReturn(command);

        DateTime now = DateTime.now(DateTimeZone.UTC);
        sut.now = () -> now;

        sut.export(MessagesRequest.withDefaults(), chunk -> {});

        ArgumentCaptor<MessagesExportEvent> eventCaptor = ArgumentCaptor.forClass(MessagesExportEvent.class);

        //noinspection UnstableApiUsage
        verify(eventBus).post(eventCaptor.capture());

        MessagesExportEvent event = eventCaptor.getValue();

        assertAll("should send event",
                () -> assertThat(event.timeRange()).isEqualTo(command.timeRange()),
                () -> assertThat(event.executionStart()).isEqualTo(now),
                () -> assertThat(event.queryString()).isEqualTo(command.queryString().queryString()),
                () -> assertThat(event.streams()).isEqualTo(command.streams()),
                () -> assertThat(event.fieldsInOrder()).isEqualTo(command.fieldsInOrder())
        );
    }

    private ArrayList<SimpleMessageChunk> exportSearchTypeWithStubbedSingleChunkFromBackend(Search s, String searchTypeId, SimpleMessageChunk chunkFromBackend) {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<SimpleMessageChunk>> captor = ArgumentCaptor.forClass(Consumer.class);

        doNothing().when(backend).run(any(), captor.capture());

        ArrayList<SimpleMessageChunk> results = new ArrayList<>();

        exportSearchType(s, searchTypeId, ResultFormat.builder().build(), results::add);

        Consumer<SimpleMessageChunk> forwarderFromBackend = captor.getValue();

        forwarderFromBackend.accept(chunkFromBackend);

        return results;
    }

    private void exportSearchType(Search search, String searchTypeId, ResultFormat resultFormat) {
        exportSearchType(search, searchTypeId, resultFormat, x -> {
        });
    }

    private void exportSearchType(Search search, String searchTypeId, ResultFormat resultFormat, Consumer<SimpleMessageChunk> forwarder) {
        sut.export(search, searchTypeId, resultFormat, forwarder);
    }

    private void exportSearch(Search search, ResultFormat resultFormat) {
        exportSearch(search, resultFormat, x -> {});
    }

    private void exportSearch(Search search, ResultFormat resultFormat, Consumer<SimpleMessageChunk> forwarder) {
        sut.export(search, resultFormat, forwarder);
    }

    private Search searchWithQueries(Query... queries) {
        return Search.builder().id("search-id")
                .queries(ImmutableSet.copyOf(queries)).build();
    }

    private TimeRange timeRange(@SuppressWarnings("SameParameterValue") int range) {
        try {
            return RelativeRange.create(range);
        } catch (InvalidRangeParametersException e) {
            throw new RuntimeException(e);
        }
    }
}
