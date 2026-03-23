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
import com.google.common.eventbus.Subscribe;
import com.swrve.ratelimitedlogger.RateLimitedLog;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineMetadataService;
import org.graylog.plugins.pipelineprocessor.events.PipelineConnectionsChangedEvent;
import org.graylog.plugins.pipelineprocessor.events.PipelinesChangedEvent;
import org.graylog.plugins.pipelineprocessor.events.RulesChangedEvent;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.rest.resources.system.inputs.InputDeletedEvent;
import org.graylog2.rest.resources.system.inputs.InputRenamedEvent;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import static org.graylog2.plugin.utilities.ratelimitedlog.RateLimitedLogFactory.createDefaultRateLimitedLog;

/**
 * Listens on {@link ClusterEventBus} for pipeline-related events and delegates metadata updates
 * to {@link PipelineMetadataUpdater}. Since ClusterEventBus only delivers events on the originating
 * node, this replaces the previous leader-only guard in PipelineInterpreterStateUpdater.
 */
@Singleton
public class PipelineMetadataClusterEventHandler {

    private static final RateLimitedLog log = createDefaultRateLimitedLog(PipelineMetadataClusterEventHandler.class);

    private final PipelineInterpreterStateBuilder stateBuilder;
    private final PipelineMetricRegistry pipelineMetricRegistry;
    private final PipelineMetadataUpdater metadataUpdater;
    private final MongoDbPipelineMetadataService pipelineMetadataService;
    private final ScheduledExecutorService executor;

    @Inject
    public PipelineMetadataClusterEventHandler(ClusterEventBus clusterEventBus,
                                               PipelineInterpreterStateBuilder stateBuilder,
                                               MetricRegistry metricRegistry,
                                               PipelineMetadataUpdater metadataUpdater,
                                               MongoDbPipelineMetadataService pipelineMetadataService,
                                               @Named("daemonScheduler") ScheduledExecutorService executor) {
        this.stateBuilder = stateBuilder;
        this.pipelineMetricRegistry = PipelineMetricRegistry.create(metricRegistry, Pipeline.class.getName(), Rule.class.getName());
        this.metadataUpdater = metadataUpdater;
        this.pipelineMetadataService = pipelineMetadataService;
        this.executor = executor;
        clusterEventBus.registerClusterEventSubscriber(this);
    }

    @Subscribe
    public void handleRuleChanges(RulesChangedEvent event) {
        executor.submit(() -> {
            try {
                final PipelineInterpreter.State state = stateBuilder.buildState(pipelineMetricRegistry);
                metadataUpdater.handleRuleChanges(event, state);
            } catch (Exception e) {
                log.warn("Failed to update pipeline metadata for rule changes: {} {}", event, e.getMessage());
            }
        });
    }

    @Subscribe
    public void handlePipelineChanges(PipelinesChangedEvent event) {
        executor.submit(() -> {
            try {
                final PipelineInterpreter.State state = stateBuilder.buildState(pipelineMetricRegistry);
                metadataUpdater.handlePipelineChanges(event, state);
            } catch (Exception e) {
                log.warn("Failed to update pipeline metadata for pipeline changes: {} {}", event, e.getMessage());
            }
        });
    }

    @Subscribe
    public void handlePipelineConnectionChanges(PipelineConnectionsChangedEvent event) {
        executor.submit(() -> {
            try {
                final PipelineInterpreter.State state = stateBuilder.buildState(pipelineMetricRegistry);
                metadataUpdater.handleConnectionChanges(event, state);
            } catch (Exception e) {
                log.warn("Failed to update pipeline metadata for connection changes: {} {}", event, e.getMessage());
            }
        });
    }

    @Subscribe
    public void handleInputDeleted(InputDeletedEvent event) {
        executor.submit(() -> {
            try {
                final PipelineInterpreter.State state = stateBuilder.buildState(pipelineMetricRegistry);
                metadataUpdater.handleInputDeleted(event, state);
            } catch (Exception e) {
                log.warn("Failed to update pipeline metadata for input deletion: {} {}", event, e.getMessage());
            }
        });
    }

    @Subscribe
    public void handleInputRenamed(InputRenamedEvent event) {
        executor.submit(() -> {
            try {
                final PipelineInterpreter.State state = stateBuilder.buildState(pipelineMetricRegistry);
                Set<RulesChangedEvent.Reference> updated = pipelineMetadataService.getReferencingPipelines().stream()
                        .flatMap(dao -> dao.rules().stream())
                        .filter(Objects::nonNull)
                        .map(ruleId -> new RulesChangedEvent.Reference(ruleId, ruleId))
                        .collect(Collectors.toSet());
                if (!updated.isEmpty()) {
                    metadataUpdater.handleRuleChanges(new RulesChangedEvent(updated, Set.of()), state);
                }
            } catch (Exception e) {
                log.warn("Failed to update pipeline metadata for input rename: {} {}", event, e.getMessage());
            }
        });
    }
}
