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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.plugins.views.search.export.LinkedHashSetUtil.linkedHashSetOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessagesExporterImplTest {

    private ExportBackend backend;
    private DecoratingMessagesExporter sut;
    private ChunkDecorator chunkDecorator;

    @BeforeEach
    void setUp() {
        backend = mock(ExportBackend.class);
        chunkDecorator = mock(ChunkDecorator.class);
        sut = new DecoratingMessagesExporter(backend, chunkDecorator);
    }

    @Test
    void appliesDecorators() {
        ExportMessagesCommand command = ExportMessagesCommand.withDefaults();

        SimpleMessageChunk undecoratedChunk = SimpleMessageChunk.from(linkedHashSetOf("field-1"), linkedHashSetOf());
        SimpleMessageChunk decoratedChunk = SimpleMessageChunk.from(linkedHashSetOf("field-1", "field-2"), linkedHashSetOf());

        when(chunkDecorator.decorate(eq(undecoratedChunk), any())).thenReturn(decoratedChunk);

        ArrayList<SimpleMessageChunk> results = exportWithStubbedSingleChunkFromBackend(command, undecoratedChunk);

        assertThat(results).containsExactly(decoratedChunk);
    }

    private ArrayList<SimpleMessageChunk> exportWithStubbedSingleChunkFromBackend(ExportMessagesCommand command, SimpleMessageChunk chunkFromBackend) {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<SimpleMessageChunk>> captor = ArgumentCaptor.forClass(Consumer.class);

        doNothing().when(backend).run(eq(command), captor.capture());

        ArrayList<SimpleMessageChunk> results = new ArrayList<>();

        sut.export(command, results::add);

        Consumer<SimpleMessageChunk> forwarderFromBackend = captor.getValue();

        forwarderFromBackend.accept(chunkFromBackend);

        return results;
    }
}
