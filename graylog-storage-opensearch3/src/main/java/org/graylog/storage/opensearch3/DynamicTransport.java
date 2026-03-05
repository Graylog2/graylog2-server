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
package org.graylog.storage.opensearch3;

import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.transport.Endpoint;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.TransportOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class DynamicTransport implements OpenSearchTransport {

    private static final Logger LOG = LoggerFactory.getLogger(DynamicTransport.class);
    private static final long DRAIN_DELAY_SECONDS = 5;

    private final AtomicReference<OpenSearchTransport> current;
    private final ScheduledExecutorService scheduler;
    private final AtomicLong swapGeneration = new AtomicLong(0);
    private volatile long drainedGeneration = 0;

    public DynamicTransport(OpenSearchTransport initial, ScheduledExecutorService scheduler) {
        this.current = new AtomicReference<>(initial);
        this.scheduler = scheduler;
    }

    public void swap(OpenSearchTransport newTransport) {
        final OpenSearchTransport old = current.getAndSet(newTransport);
        final long generation = swapGeneration.incrementAndGet();
        LOG.info("OpenSearch transport swapped due to node list update (generation {}). Draining old transport.", generation);
        scheduler.schedule(() -> {
            drainedGeneration = generation;
            try {
                old.close();
            } catch (IOException e) {
                LOG.warn("Failed to close old OpenSearch transport after drain period", e);
            }
        }, DRAIN_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    private boolean isSwapping() {
        return swapGeneration.get() > drainedGeneration;
    }

    @Override
    public <RequestT, ResponseT, ErrorT> ResponseT performRequest(
            RequestT request,
            Endpoint<RequestT, ResponseT, ErrorT> endpoint,
            TransportOptions options) throws IOException {
        try {
            return current.get().performRequest(request, endpoint, options);
        } catch (IOException e) {
            throw maybeWrapException(e);
        }
    }

    @Override
    public <RequestT, ResponseT, ErrorT> CompletableFuture<ResponseT> performRequestAsync(
            RequestT request,
            Endpoint<RequestT, ResponseT, ErrorT> endpoint,
            TransportOptions options) {
        return current.get().performRequestAsync(request, endpoint, options)
                .exceptionally(t -> {
                    if (isSwapping() && t instanceof IOException) {
                        throw new RuntimeException(wrapMessage(t.getMessage()), t);
                    }
                    if (t instanceof RuntimeException re) {
                        throw re;
                    }
                    throw new RuntimeException(t);
                });
    }

    @Override
    public JsonpMapper jsonpMapper() {
        return current.get().jsonpMapper();
    }

    @Override
    public TransportOptions options() {
        return current.get().options();
    }

    @Override
    public void close() throws IOException {
        current.get().close();
    }

    private IOException maybeWrapException(IOException original) {
        if (isSwapping()) {
            return new IOException(wrapMessage(original.getMessage()), original);
        }
        return original;
    }

    private static String wrapMessage(String originalMessage) {
        return "Request failed during node list update. This is likely transient. Original error: " + originalMessage;
    }
}
