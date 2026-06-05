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
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudCollectorIngestServiceTest {
    private static final long AWAIT_MS = 5_000;
    private static final long QUIET_MS = 300;

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

        service = new CloudCollectorIngestService(inputBuffer, eventBus, configService, httpInputFactory);
    }

    @AfterEach
    void tearDown() throws TimeoutException {
        if (service == null) {
            return;
        }
        // The service runs on a non-daemon thread, so it must be stopped after every test (pass or fail) to avoid
        // leaking a live thread into the surefire JVM fork.
        service.stopAsync();
        try {
            service.awaitTerminated(AWAIT_MS, TimeUnit.MILLISECONDS);
        } catch (IllegalStateException serviceAlreadyFailed) {
            // The service ended in FAILED (a test exercised a fatal path). Its execution thread has already exited,
            // so there is nothing to clean up — and we must not mask the test's own assertion failure.
        }
        // A TimeoutException is intentionally NOT caught: it means the service thread did not stop — a real
        // shutdown bug / thread leak worth failing on.
    }

    @Test
    void launchesInputWhenConfigAlreadyPresent() throws Exception {
        when(configService.get()).thenReturn(Optional.of(config()));

        service.startAsync().awaitRunning();

        verify(input, timeout(AWAIT_MS)).initialize();
        verify(input, timeout(AWAIT_MS)).launch(eq(inputBuffer), any(InputFailureRecorder.class));
        verify(input).setPersistId(ReservedInputIds.EPHEMERAL_COLLECTOR_INGEST);
    }

    @Test
    void waitsForConfigThenLaunchesOnConfigChangedEvent() throws Exception {
        // Absent on the first check, present once the config-changed event wakes the service.
        when(configService.get())
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(config()));

        service.startAsync().awaitRunning();

        // While the config is absent, the input must not be launched.
        verify(input, after(QUIET_MS).never()).launch(any(), any());

        // A CollectorsConfig change (the user saving the config) wakes the service.
        eventBus.post(ClusterConfigChangedEvent.create(
                DateTime.now(DateTimeZone.UTC), "node-id", CollectorsConfig.class.getCanonicalName()));

        verify(input, timeout(AWAIT_MS)).launch(eq(inputBuffer), any(InputFailureRecorder.class));
    }

    @Test
    void ignoresConfigChangedEventsForOtherConfigTypes() throws Exception {
        when(configService.get()).thenReturn(Optional.empty());

        service.startAsync().awaitRunning();

        // An unrelated cluster-config change must not release the wait.
        eventBus.post(ClusterConfigChangedEvent.create(
                DateTime.now(DateTimeZone.UTC), "node-id", "org.graylog2.some.OtherConfig"));

        verify(input, after(QUIET_MS).never()).launch(any(), any());
    }

    @Test
    void doesNotLaunchAndTerminatesWhenShutDownWhileWaiting() throws Exception {
        when(configService.get()).thenReturn(Optional.empty());

        service.startAsync().awaitRunning();
        verify(input, after(QUIET_MS).never()).launch(any(), any());

        // Interrupt-driven shutdown must break out of the wait cleanly.
        service.stopAsync().awaitTerminated();

        verify(input, never()).launch(any(), any());
        verify(input, never()).stop();
    }

    @Test
    void stopsInputOnShutdownAfterLaunch() throws Exception {
        when(configService.get()).thenReturn(Optional.of(config()));

        service.startAsync().awaitRunning();
        verify(input, timeout(AWAIT_MS)).launch(any(), any());

        service.stopAsync().awaitTerminated();

        verify(input, timeout(AWAIT_MS)).stop();
    }

    @Test
    void retriesLaunchOnMisfire() throws Exception {
        when(configService.get()).thenReturn(Optional.of(config()));
        // First launch fails (transient bind failure), second succeeds.
        doThrow(new MisfireException("boom")).doNothing().when(input).launch(any(), any());

        service.startAsync().awaitRunning();

        verify(input, timeout(AWAIT_MS).times(2)).launch(eq(inputBuffer), any(InputFailureRecorder.class));
    }

    private static CollectorsConfig config() {
        return CollectorsConfig.createDefault("graylog.example.com");
    }
}
