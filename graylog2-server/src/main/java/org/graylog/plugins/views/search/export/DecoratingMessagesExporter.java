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

import javax.inject.Inject;
import java.util.function.Consumer;

public class DecoratingMessagesExporter implements MessagesExporter {
    private final ExportBackend backend;
    private final ChunkDecorator chunkDecorator;

    @Inject
    public DecoratingMessagesExporter(
            ExportBackend backend,
            ChunkDecorator chunkDecorator) {
        this.backend = backend;
        this.chunkDecorator = chunkDecorator;
    }

    public void export(ExportMessagesCommand command, Consumer<SimpleMessageChunk> chunkForwarder) {
        Consumer<SimpleMessageChunk> decoratedForwarder = chunk -> decorate(chunkForwarder, chunk, command);

        backend.run(command, decoratedForwarder);
    }

    private void decorate(Consumer<SimpleMessageChunk> chunkForwarder, SimpleMessageChunk chunk, ExportMessagesCommand command) {
        SimpleMessageChunk decoratedChunk = chunkDecorator.decorate(chunk, command);

        chunkForwarder.accept(decoratedChunk);
    }
}
