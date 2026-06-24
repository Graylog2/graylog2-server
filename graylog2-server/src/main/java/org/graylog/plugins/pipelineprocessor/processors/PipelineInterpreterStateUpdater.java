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
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.swrve.ratelimitedlogger.RateLimitedLog;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.events.PipelineConnectionsChangedEvent;
import org.graylog.plugins.pipelineprocessor.events.PipelinesChangedEvent;
import org.graylog.plugins.pipelineprocessor.events.RuleMetricsConfigChangedEvent;
import org.graylog.plugins.pipelineprocessor.events.RulesChangedEvent;
import org.graylog2.rest.resources.system.inputs.InputDeletedEvent;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.graylog2.plugin.utilities.ratelimitedlog.RateLimitedLogFactory.createDefaultRateLimitedLog;

@Singleton
public class PipelineInterpreterStateUpdater {
    private static final RateLimitedLog log = createDefaultRateLimitedLog(PipelineInterpreterStateUpdater.class);

    private final PipelineInterpreterStateBuilder stateBuilder;
    private final ScheduledExecutorService scheduler;
    /**
     * non-null if the update has successfully loaded a state
     */
    private final AtomicReference<PipelineInterpreter.State> latestState = new AtomicReference<>();
    private final PipelineMetricRegistry pipelineMetricRegistry;

    @Inject
    public PipelineInterpreterStateUpdater(PipelineInterpreterStateBuilder stateBuilder,
                                     MetricRegistry metricRegistry,
                                     @Named("daemonScheduler") ScheduledExecutorService scheduler,
                                     EventBus serverEventBus) {
        this.stateBuilder = stateBuilder;
        this.scheduler = scheduler;
        this.pipelineMetricRegistry = PipelineMetricRegistry.create(metricRegistry, Pipeline.class.getName(), Rule.class.getName());

        // Perform the synchronous initial load before registering on the event bus. This closes the race
        // window where an async event could trigger a reload before the initial load completes. A failure
        // here (e.g. a transient MongoDB error) schedules a local retry rather than aborting startup.
        reloadAndSave();

        // listens to cluster wide Rule, Pipeline and pipeline stream connection changes
        serverEventBus.register(this);
    }

    // Only the singleton instance should mutate itself. We reload locally on every node so that each node
    // rebuilds its own in-memory state. On a transient failure we retry on the local scheduler so we never
    // get stuck on stale state, and we route the result through updateState() to apply the empty-state guard.
    private synchronized void reloadAndSave() {
        try {
            final PipelineInterpreter.State newState = stateBuilder.buildState(pipelineMetricRegistry);
            updateState(newState);
        } catch (Exception e) {
            log.warn("Failed to reload pipeline interpreter state, retrying in 1 second", e);
            scheduler.schedule(this::reloadAndSave, 1, TimeUnit.SECONDS);
        }
    }

    /**
     * Updates the pipeline interpreter state. Refuses to replace a non-empty state with an empty
     * one as a defense-in-depth measure against transient MongoDB failures producing empty results.
     *
     * @param newState the new state to set
     */
    public synchronized void updateState(PipelineInterpreter.State newState) {
        final PipelineInterpreter.State currentState = latestState.get();
        if (currentState != null && !isEmptyState(currentState) && isEmptyState(newState)) {
            log.warn("Refusing to replace non-empty pipeline state with empty state. " +
                    "This is likely caused by a transient MongoDB error. Current state has {} pipelines.",
                    currentState.getCurrentPipelines().size());
            return;
        }
        latestState.set(newState);
        log.debug("Pipeline interpreter state got updated");
    }

    private static boolean isEmptyState(PipelineInterpreter.State state) {
        return state.getCurrentPipelines().isEmpty() && state.getStreamPipelineConnections().isEmpty();
    }

    /**
     * Can be used to inspect or use the current state of the pipeline system.
     * For example, the interpreter
     *
     * @return the currently loaded state of the updater
     */
    public PipelineInterpreter.State getLatestState() {
        return latestState.get();
    }

    private void scheduleReload() {
        scheduler.schedule(this::reloadAndSave, 0, TimeUnit.SECONDS);
    }

    // TODO avoid reloading everything on every change, certain changes can get away with doing less work
    @Subscribe
    public void handleRuleChanges(RulesChangedEvent event) {
        event.deletedRules().forEach(ref -> {
            log.debug("Invalidated rule {}", ref.id());
            pipelineMetricRegistry.removeRuleMetrics(ref.id());
        });
        event.updatedRules().forEach(ref -> log.debug("Refreshing rule {}", ref.id()));
        scheduleReload();
    }

    @Subscribe
    public void handlePipelineChanges(PipelinesChangedEvent event) {
        event.deletedPipelineIds().forEach(id -> {
            log.debug("Invalidated pipeline {}", id);
            pipelineMetricRegistry.removePipelineMetrics(id);
        });
        event.updatedPipelineIds().forEach(id -> log.debug("Refreshing pipeline {}", id));
        scheduleReload();
    }

    @Subscribe
    public void handlePipelineConnectionChanges(PipelineConnectionsChangedEvent event) {
        log.debug("Pipeline stream connection changed: {}", event);
        scheduleReload();
    }

    @Subscribe
    public void handleRuleMetricsConfigChange(RuleMetricsConfigChangedEvent event) {
        log.debug("Rule metrics config changed: {}", event);
        scheduleReload();
    }

    @Subscribe
    public void handleInputDeleted(InputDeletedEvent event) {
        log.debug("Input deleted: {}", event);
        scheduleReload();
    }
}
