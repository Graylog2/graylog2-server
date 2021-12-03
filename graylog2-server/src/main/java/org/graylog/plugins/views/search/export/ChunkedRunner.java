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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.glassfish.jersey.server.ChunkedOutput;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
                writeExceptionAsChunk(ex);
            } finally {
               close();
            }
        });
    }

    private void writeExceptionAsChunk(Exception ex) {
        // get to the underlying cause
        Throwable cause = ExceptionUtils.getRootCause(ex, true);

        if (cause.getSuppressed().length > 0) {
            cause = cause.getSuppressed()[0];
        }

        write(createErrChunk(cause));
    }

    private SimpleMessageChunk createErrChunk(Throwable cause) {
        final LinkedHashMap<String, Object> err = new LinkedHashMap<>();
        err.put("err", cause.toString());
        final LinkedHashSet<SimpleMessage> messages = new LinkedHashSet<>();
        messages.add(SimpleMessage.builder().index("err").fields(err).build());
        return SimpleMessageChunk.builder()
                .fieldsInOrder(new LinkedHashSet<>(Collections.singletonList("err")))
                .chunkOrder(SimpleMessageChunk.ChunkOrder.LAST)
                .messages(messages)
                .build();
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
