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
