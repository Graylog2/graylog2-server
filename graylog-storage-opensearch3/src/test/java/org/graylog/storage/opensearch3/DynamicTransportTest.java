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

import org.junit.jupiter.api.Test;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.transport.Endpoint;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.TransportOptions;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DynamicTransportTest {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        final Thread t = new Thread(r, "test-drain-scheduler");
        t.setDaemon(true);
        return t;
    });

    @Test
    void delegatesJsonpMapperToCurrentTransport() {
        final var delegate = mock(OpenSearchTransport.class);
        final var mapper = mock(JsonpMapper.class);
        when(delegate.jsonpMapper()).thenReturn(mapper);

        final var transport = new DynamicTransport(delegate, scheduler);
        assertThat(transport.jsonpMapper()).isSameAs(mapper);
    }

    @Test
    void delegatesOptionsToCurrentTransport() {
        final var delegate = mock(OpenSearchTransport.class);
        final var options = mock(TransportOptions.class);
        when(delegate.options()).thenReturn(options);

        final var transport = new DynamicTransport(delegate, scheduler);
        assertThat(transport.options()).isSameAs(options);
    }

    @Test
    @SuppressWarnings("unchecked")
    void delegatesPerformRequestToCurrentTransport() throws IOException {
        final var delegate = mock(OpenSearchTransport.class);
        final var endpoint = mock(Endpoint.class);
        final var options = mock(TransportOptions.class);
        when(delegate.performRequest(any(), any(), any())).thenReturn("result");

        final var transport = new DynamicTransport(delegate, scheduler);
        final Object result = transport.performRequest("req", endpoint, options);
        assertThat(result).isEqualTo("result");
    }

    @Test
    @SuppressWarnings("unchecked")
    void swapChangesDelegate() throws IOException {
        final var oldDelegate = mock(OpenSearchTransport.class);
        final var newDelegate = mock(OpenSearchTransport.class);
        final var endpoint = mock(Endpoint.class);
        final var options = mock(TransportOptions.class);
        when(newDelegate.performRequest(any(), any(), any())).thenReturn("new-result");

        final var transport = new DynamicTransport(oldDelegate, scheduler);
        transport.swap(newDelegate);

        final Object result = transport.performRequest("req", endpoint, options);
        assertThat(result).isEqualTo("new-result");
    }

    @Test
    void closeDoesNotCloseOldTransportImmediatelyOnSwap() throws IOException {
        final var oldDelegate = mock(OpenSearchTransport.class);
        final var newDelegate = mock(OpenSearchTransport.class);

        final var transport = new DynamicTransport(oldDelegate, scheduler);
        transport.swap(newDelegate);

        // Old transport should not be closed immediately (graceful drain)
        verify(oldDelegate, never()).close();
    }

    @Test
    @SuppressWarnings("unchecked")
    void wrapsExceptionDuringSwapWindow() throws IOException {
        final var oldDelegate = mock(OpenSearchTransport.class);
        final var newDelegate = mock(OpenSearchTransport.class);
        final var endpoint = mock(Endpoint.class);
        final var options = mock(TransportOptions.class);
        when(newDelegate.performRequest(any(), any(), any())).thenThrow(new IOException("connection reset"));

        // Use a scheduler that never runs drain tasks (so isSwapping() stays true)
        final var neverRunScheduler = mock(ScheduledExecutorService.class);
        final var transport = new DynamicTransport(oldDelegate, neverRunScheduler);
        transport.swap(newDelegate);

        assertThatThrownBy(() -> transport.performRequest("req", endpoint, options))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("node list update");
    }

    @Test
    @SuppressWarnings("unchecked")
    void asyncWrapsIOExceptionInsideCompletionExceptionDuringSwap() {
        final var oldDelegate = mock(OpenSearchTransport.class);
        final var newDelegate = mock(OpenSearchTransport.class);
        final var endpoint = mock(Endpoint.class);
        final var options = mock(TransportOptions.class);
        // Simulate how CompletableFuture delivers exceptions: wrapped in CompletionException
        when(newDelegate.performRequestAsync(any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new IOException("connection reset")));

        final var neverRunScheduler = mock(ScheduledExecutorService.class);
        final var transport = new DynamicTransport(oldDelegate, neverRunScheduler);
        transport.swap(newDelegate);

        final CompletableFuture<?> future = transport.performRequestAsync("req", endpoint, options);
        // CompletableFuture.get() unwraps CompletionException, so the chain is ExecutionException -> IOException
        assertThatThrownBy(future::get)
                .isInstanceOf(ExecutionException.class)
                .cause()
                .isInstanceOf(IOException.class)
                .hasMessageContaining("node list update")
                .hasMessageContaining("connection reset");
    }

    @Test
    @SuppressWarnings("unchecked")
    void asyncPreservesOriginalExceptionWhenNotSwapping() {
        final var delegate = mock(OpenSearchTransport.class);
        final var endpoint = mock(Endpoint.class);
        final var options = mock(TransportOptions.class);
        final var originalException = new IOException("original error");
        when(delegate.performRequestAsync(any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(originalException));

        // No swap has occurred, so isSwapping() is false
        final var transport = new DynamicTransport(delegate, scheduler);

        final CompletableFuture<?> future = transport.performRequestAsync("req", endpoint, options);
        // Original IOException is preserved as-is for downstream handlers
        assertThatThrownBy(future::get)
                .isInstanceOf(ExecutionException.class)
                .cause()
                .isSameAs(originalException);
    }

    @Test
    void closesOldTransportImmediatelyWhenSchedulerRejects() throws IOException {
        final var oldDelegate = mock(OpenSearchTransport.class);
        final var newDelegate = mock(OpenSearchTransport.class);

        final var rejectedScheduler = mock(ScheduledExecutorService.class);
        when(rejectedScheduler.schedule(any(Runnable.class), anyLong(), any()))
                .thenThrow(new RejectedExecutionException("shutdown"));

        final var transport = new DynamicTransport(oldDelegate, rejectedScheduler);

        assertThatCode(() -> transport.swap(newDelegate)).doesNotThrowAnyException();
        verify(oldDelegate).close();
    }
}
