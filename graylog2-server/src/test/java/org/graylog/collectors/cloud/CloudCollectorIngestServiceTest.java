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
package org.graylog.collectors.cloud;

import com.google.common.eventbus.EventBus;
import org.graylog.collectors.CollectorsConfig;
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.input.CollectorIngestHttpInput;
import org.graylog2.cluster.ClusterConfigChangedEvent;
import org.graylog2.inputs.ReservedInputIds;
import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.plugin.buffers.InputBuffer;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.MisfireException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudCollectorIngestServiceTest {
    private static final long AWAIT_MS = 5_000;
    private static final int SHUTDOWN_TIMEOUT_MS = 300;

    @Mock
    private InputBuffer inputBuffer;
    @Mock
    private CollectorsConfigService configService;
    @Mock
    private CollectorIngestHttpInput.Factory httpInputFactory;
    @Mock
    private CollectorIngestHttpInput.Config inputConfig;
    @Mock
    private CollectorIngestHttpInput input;

    private final EventBus eventBus = new EventBus();

    private CloudCollectorIngestService service;

    @BeforeEach
    void setUp() {
        // Used only by tests that actually launch the input — lenient so the "never launches" tests don't trip
        // strict-stub checking.
        lenient().when(httpInputFactory.getConfig()).thenReturn(inputConfig);
        lenient().when(inputConfig.combinedRequestedConfiguration()).thenReturn(new ConfigurationRequest());
        lenient().when(httpInputFactory.create(any())).thenReturn(input);

        service = new CloudCollectorIngestService(inputBuffer, eventBus, configService, httpInputFactory,
                SHUTDOWN_TIMEOUT_MS);
    }

    @AfterEach
    void tearDown() throws TimeoutException {
        if (service == null) {
            return;
        }
        // The service's launcher executor uses a non-daemon thread, so the service must be stopped after every test
        // (pass or fail) to avoid leaking a live thread into the surefire JVM fork.
        service.stopAsync();
        service.awaitTerminated(AWAIT_MS, TimeUnit.MILLISECONDS);
        // A TimeoutException is intentionally NOT caught: it means the service did not stop — a real
        // shutdown bug / thread leak worth failing on.
    }

    @Test
    void launchesInputWhenConfigAlreadyPresent() throws Exception {
        when(configService.get()).thenReturn(Optional.of(config()));

        service.startAsync().awaitRunning();
        awaitIdle();

        verify(input).initialize();
        verify(input).launch(eq(inputBuffer), any(InputFailureRecorder.class));
        verify(input).setPersistId(ReservedInputIds.EPHEMERAL_COLLECTOR_INGEST);
    }

    @Test
    void waitsForConfigThenLaunchesOnConfigChangedEvent() throws Exception {
        // Absent on the startup check, present once the config-changed event triggers the next launch attempt.
        when(configService.get())
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(config()));

        service.startAsync().awaitRunning();

        // While the config is absent, the input must not be launched.
        awaitIdle();
        verify(input, never()).launch(any(), any());

        // A CollectorsConfig change (the user saving the config) triggers another launch attempt.
        eventBus.post(ClusterConfigChangedEvent.create(
                DateTime.now(DateTimeZone.UTC), "node-id", CollectorsConfig.class.getCanonicalName()));

        awaitIdle();
        verify(input).launch(eq(inputBuffer), any(InputFailureRecorder.class));
    }

    @Test
    void ignoresConfigChangedEventsForOtherConfigTypes() throws Exception {
        when(configService.get()).thenReturn(Optional.empty());

        service.startAsync().awaitRunning();

        // An unrelated cluster-config change must not trigger a launch attempt.
        eventBus.post(ClusterConfigChangedEvent.create(
                DateTime.now(DateTimeZone.UTC), "node-id", "org.graylog2.some.OtherConfig"));

        awaitIdle();
        verify(input, never()).launch(any(), any());
    }

    @Test
    void doesNotLaunchAndTerminatesWhenShutDownBeforeConfigExists() throws Exception {
        when(configService.get()).thenReturn(Optional.empty());

        service.startAsync().awaitRunning();
        // Let the startup launch attempt run (it finds no config and gives up).
        awaitIdle();

        // Shutting down the idle service (no config yet) must terminate cleanly without launching or stopping
        // an input.
        service.stopAsync().awaitTerminated();

        verify(input, never()).launch(any(), any());
        verify(input, never()).stop();
    }

    @Test
    void stopsInputOnShutdownAfterLaunch() throws Exception {
        when(configService.get()).thenReturn(Optional.of(config()));

        service.startAsync().awaitRunning();
        awaitIdle();
        verify(input).launch(any(), any());

        service.stopAsync().awaitTerminated();

        // stop() happens synchronously within shutDown() (after the launcher executor has terminated), so once
        // awaitTerminated() returns it must already have been called — no timeout-wait needed.
        verify(input).stop();
    }

    @Test
    void stopsFailedInputBeforeRetryingOnMisfire() throws Exception {
        when(configService.get()).thenReturn(Optional.of(config()));

        // Each attempt builds a fresh input. The first misfires (e.g. transient bind failure); the second succeeds.
        final var firstInput = mock(CollectorIngestHttpInput.class);
        final var secondInput = mock(CollectorIngestHttpInput.class);
        when(httpInputFactory.create(any())).thenReturn(firstInput, secondInput);
        doThrow(new MisfireException("boom")).when(firstInput).launch(any(), any());

        service.startAsync().awaitRunning();
        // awaitIdle() returns only after the whole launch attempt — including its retries — has finished.
        awaitIdle();

        // The misfired input must be stopped (releasing its port and de-registering its metrics) before the next
        // attempt launches — otherwise the re-registered metrics collide and every retry fails.
        final var inOrder = inOrder(firstInput, secondInput);
        inOrder.verify(firstInput).launch(eq(inputBuffer), any(InputFailureRecorder.class));
        inOrder.verify(firstInput).stop();
        inOrder.verify(secondInput).launch(eq(inputBuffer), any(InputFailureRecorder.class));

        // The successfully launched input is left running; it is only stopped on shutdown.
        verify(secondInput, never()).stop();
    }

    @Test
    void doesNotRetryNonMisfireFailures() throws Exception {
        when(configService.get()).thenReturn(Optional.of(config()));
        // Only MisfireExceptions are retried. A non-transient failure (here a RuntimeException) must be logged and
        // given up on — not retried forever.
        doThrow(new RuntimeException("boom")).when(input).launch(any(), any());

        service.startAsync().awaitRunning();
        awaitIdle();

        // Exactly one attempt (default verify count) — no retry. If the retry predicate caught this, the loop would
        // never settle and awaitIdle() would time out.
        verify(input).launch(eq(inputBuffer), any(InputFailureRecorder.class));
    }

    // Deterministic barrier replacing timeout()/after() waits: returns once every launch attempt submitted so far
    // has completed.
    private void awaitIdle() throws Exception {
        //noinspection resource
        service.executorService().submit(() -> {}).get(AWAIT_MS, TimeUnit.MILLISECONDS);
    }

    private static CollectorsConfig config() {
        return CollectorsConfig.createDefault("graylog.example.com");
    }
}
