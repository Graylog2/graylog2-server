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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.glassfish.jersey.server.ChunkedOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ChunkedRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ChunkedRunner.class);

    private final ChunkedOutput<SimpleMessageChunk> output = new ChunkedOutput<>(SimpleMessageChunk.class);

    public static ChunkedOutput<SimpleMessageChunk> runAsync(Consumer<Consumer<SimpleMessageChunk>> call) {

        ChunkedRunner r = new ChunkedRunner();
        r.run(call);

        return r.output;
    }

    private void run(Consumer<Consumer<SimpleMessageChunk>> call) {
        ExecutorService e = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("chunked-messages-request").build());

        e.submit(() -> {
            try {
                call.accept(this::write);
            } catch (Exception ex) {
                LOG.error("Error executing runnable", ex);
            } finally {
                close();
            }
        });
    }

    private void close() {
        try {
            output.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to close ChunkedOutput", e);
        }
    }

    private void write(SimpleMessageChunk chunk) {
        try {
            output.write(chunk);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to ChunkedOutput", e);
        }
    }
}
