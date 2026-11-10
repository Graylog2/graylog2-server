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
package org.graylog.plugins.pipelineprocessor.processors;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.eventbus.EventBus;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.events.PipelineConnectionsChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the hardening behaviour added to {@link PipelineInterpreterStateUpdater}: the empty-state guard,
 * retry-on-failure, and coalescing of reload requests. These tests drive the updater directly with a mocked
 * {@link PipelineInterpreterStateBuilder} and a mocked scheduler whose runnables are captured and invoked by hand,
 * so they exercise the real control flow without touching MongoDB or waiting on wall-clock delays.
 */
class PipelineInterpreterStateUpdaterTest {

    private PipelineInterpreterStateBuilder stateBuilder;
    private ScheduledExecutorService scheduler;
    private EventBus serverEventBus;
    private MetricRegistry metricRegistry;

    @BeforeEach
    void setUp() {
        stateBuilder = mock(PipelineInterpreterStateBuilder.class);
        scheduler = mock(ScheduledExecutorService.class);
        serverEventBus = mock(EventBus.class);
        metricRegistry = new MetricRegistry();
    }

    private PipelineInterpreterStateUpdater newUpdater() {
        return new PipelineInterpreterStateUpdater(stateBuilder, metricRegistry, scheduler, serverEventBus);
    }

    private PipelineInterpreter.State emptyState() {
        final PipelineInterpreter.State state = mock(PipelineInterpreter.State.class);
        when(state.getCurrentPipelines()).thenReturn(ImmutableMap.of());
        when(state.getStreamPipelineConnections()).thenReturn(ImmutableSetMultimap.of());
        return state;
    }

    private PipelineInterpreter.State nonEmptyState() {
        // Create every mock up front: a mock() call between when() and thenReturn() would trip Mockito's
        // UnfinishedStubbing detection.
        final Pipeline pipeline = mock(Pipeline.class);
        final ImmutableMap<String, Pipeline> pipelines = ImmutableMap.of("p1", pipeline);
        final PipelineInterpreter.State state = mock(PipelineInterpreter.State.class);
        when(state.getCurrentPipelines()).thenReturn(pipelines);
        when(state.getStreamPipelineConnections()).thenReturn(ImmutableSetMultimap.of());
        return state;
    }

    @Test
    void constructorPerformsSynchronousInitialLoadThenRegistersOnEventBus() {
        final PipelineInterpreter.State initial = nonEmptyState();
        when(stateBuilder.buildState(any())).thenReturn(initial);

        final PipelineInterpreterStateUpdater updater = newUpdater();

        assertThat(updater.getLatestState()).isSameAs(initial);
        verify(serverEventBus).register(updater);
    }

    @Test
    void updateStateRefusesToReplaceNonEmptyStateWithEmptyState() {
        final PipelineInterpreter.State initial = nonEmptyState();
        when(stateBuilder.buildState(any())).thenReturn(initial);
        final PipelineInterpreterStateUpdater updater = newUpdater();

        updater.updateState(emptyState());

        // The transient-empty result is rejected; the previously loaded non-empty state is retained.
        assertThat(updater.getLatestState()).isSameAs(initial);
    }

    @Test
    void updateStateAllowsReplacingNonEmptyStateWithAnotherNonEmptyState() {
        final PipelineInterpreter.State initial = nonEmptyState();
        when(stateBuilder.buildState(any())).thenReturn(initial);
        final PipelineInterpreterStateUpdater updater = newUpdater();

        final PipelineInterpreter.State next = nonEmptyState();
        updater.updateState(next);

        assertThat(updater.getLatestState()).isSameAs(next);
    }

    @Test
    void updateStateAllowsEmptyStateWhenNoNonEmptyStateWasLoadedYet() {
        // A genuinely empty installation must be allowed to settle on an empty state.
        final PipelineInterpreter.State initialEmpty = emptyState();
        when(stateBuilder.buildState(any())).thenReturn(initialEmpty);
        final PipelineInterpreterStateUpdater updater = newUpdater();

        assertThat(updater.getLatestState()).isSameAs(initialEmpty);

        final PipelineInterpreter.State anotherEmpty = emptyState();
        updater.updateState(anotherEmpty);
        assertThat(updater.getLatestState()).isSameAs(anotherEmpty);
    }

    @Test
    void updateStateAllowsRecoveryFromEmptyToNonEmpty() {
        final PipelineInterpreter.State initialEmpty = emptyState();
        when(stateBuilder.buildState(any())).thenReturn(initialEmpty);
        final PipelineInterpreterStateUpdater updater = newUpdater();

        final PipelineInterpreter.State recovered = nonEmptyState();
        updater.updateState(recovered);

        assertThat(updater.getLatestState()).isSameAs(recovered);
    }

    @Test
    void failedInitialLoadSchedulesRetryThatEventuallySucceeds() {
        final PipelineInterpreter.State recovered = nonEmptyState();
        when(stateBuilder.buildState(any()))
                .thenThrow(new IllegalStateException("transient MongoDB error"))
                .thenReturn(recovered);

        final PipelineInterpreterStateUpdater updater = newUpdater();

        // Initial load threw, so no state is set yet and a 1-second retry was scheduled.
        assertThat(updater.getLatestState()).isNull();
        final ArgumentCaptor<Runnable> retry = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(retry.capture(), eq(1L), eq(TimeUnit.SECONDS));

        // Running the retry rebuilds the state successfully.
        retry.getValue().run();
        assertThat(updater.getLatestState()).isSameAs(recovered);
    }

    @Test
    void burstOfChangeEventsCoalescesIntoASingleReloadAndReArmsAfterItRuns() {
        final PipelineInterpreter.State initial = nonEmptyState();
        when(stateBuilder.buildState(any())).thenReturn(initial);
        final PipelineInterpreterStateUpdater updater = newUpdater();

        // A burst of events while no reload is pending must result in exactly one scheduled reload.
        updater.handlePipelineConnectionChanges(mock(PipelineConnectionsChangedEvent.class));
        updater.handlePipelineConnectionChanges(mock(PipelineConnectionsChangedEvent.class));
        updater.handlePipelineConnectionChanges(mock(PipelineConnectionsChangedEvent.class));

        final ArgumentCaptor<Runnable> reload = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler, times(1)).schedule(reload.capture(), eq(0L), eq(TimeUnit.SECONDS));

        // Once the queued reload actually runs it clears the pending flag, so the next event schedules again.
        reload.getValue().run();
        updater.handlePipelineConnectionChanges(mock(PipelineConnectionsChangedEvent.class));

        verify(scheduler, times(2)).schedule(any(Runnable.class), eq(0L), eq(TimeUnit.SECONDS));
    }
}
