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

import java.util.function.Consumer;

public class ChunkForwarder<T> {
    private final Consumer<T> onChunk;
    private final Runnable onClosed;

    public static <T> ChunkForwarder<T> create(Consumer<T> onChunk, Runnable onClosed) {
        return new ChunkForwarder<>(onChunk, onClosed);
    }

    public ChunkForwarder(Consumer<T> onChunk, Runnable onDone) {
        this.onChunk = onChunk;
        this.onClosed = onDone;
    }

    public void write(T chunk) {
        onChunk.accept(chunk);
    }

    public void close() {
        onClosed.run();
    }
}
