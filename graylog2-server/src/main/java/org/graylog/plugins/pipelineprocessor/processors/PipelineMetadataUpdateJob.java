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
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.inject.assistedinject.AssistedInject;
import com.swrve.ratelimitedlogger.RateLimitedLog;
import jakarta.annotation.Nullable;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineMetadataService;
import org.graylog.plugins.pipelineprocessor.events.PipelineConnectionsChangedEvent;
import org.graylog.plugins.pipelineprocessor.events.PipelinesChangedEvent;
import org.graylog.plugins.pipelineprocessor.events.RulesChangedEvent;
import org.graylog.scheduler.JobExecutionException;
import org.graylog.scheduler.system.SystemJob;
import org.graylog.scheduler.system.SystemJobConfig;
import org.graylog.scheduler.system.SystemJobContext;
import org.graylog.scheduler.system.SystemJobInfo;
import org.graylog.scheduler.system.SystemJobResult;
import org.graylog2.rest.resources.system.inputs.InputDeletedEvent;
import org.graylog2.rest.resources.system.inputs.InputRenamedEvent;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.plugin.utilities.ratelimitedlog.RateLimitedLogFactory.createDefaultRateLimitedLog;

/**
 * System scheduler job that performs pipeline metadata updates. Submitted by
 * {@link PipelineMetadataClusterEventHandler} in response to pipeline-related cluster events.
 */
public class PipelineMetadataUpdateJob implements SystemJob<PipelineMetadataUpdateJob.Config> {

    public static final String TYPE_NAME = "pipeline-metadata-update";

    private static final RateLimitedLog log = createDefaultRateLimitedLog(PipelineMetadataUpdateJob.class);

    private final PipelineInterpreterStateBuilder stateBuilder;
    private final PipelineMetadataUpdater metadataUpdater;
    private final MongoDbPipelineMetadataService pipelineMetadataService;
    private final PipelineMetricRegistry pipelineMetricRegistry;

    public interface Factory extends SystemJob.Factory<PipelineMetadataUpdateJob> {
        @Override
        PipelineMetadataUpdateJob create();
    }

    @AssistedInject
    public PipelineMetadataUpdateJob(PipelineInterpreterStateBuilder stateBuilder,
                                     PipelineMetadataUpdater metadataUpdater,
                                     MongoDbPipelineMetadataService pipelineMetadataService,
                                     MetricRegistry metricRegistry) {
        this.stateBuilder = stateBuilder;
        this.metadataUpdater = metadataUpdater;
        this.pipelineMetadataService = pipelineMetadataService;
        this.pipelineMetricRegistry = PipelineMetricRegistry.create(metricRegistry, Pipeline.class.getName(), Rule.class.getName());
    }

    @Override
    public SystemJobResult execute(Config config, SystemJobContext ctx) throws JobExecutionException {
        try {
            final PipelineInterpreter.State state = stateBuilder.buildState(pipelineMetricRegistry);
            switch (config.eventType()) {
                case RULES_CHANGED -> metadataUpdater.handleRuleChanges(config.rulesEvent(), state);
                case PIPELINES_CHANGED -> metadataUpdater.handlePipelineChanges(config.pipelinesEvent(), state);
                case PIPELINE_CONNECTIONS_CHANGED -> metadataUpdater.handleConnectionChanges(config.connectionsEvent(), state);
                case INPUT_DELETED -> metadataUpdater.handleInputDeleted(config.inputDeletedEvent(), state);
                case INPUT_RENAMED -> handleInputRenamed(state);
            }
            return SystemJobResult.success();
        } catch (Exception e) {
            log.warn("Failed to update pipeline metadata for {}: {}", config.eventType(), e.getMessage());
            return SystemJobResult.withError();
        }
    }

    private void handleInputRenamed(PipelineInterpreter.State state) {
        Set<RulesChangedEvent.Reference> updated = pipelineMetadataService.getReferencingPipelines().stream()
                .flatMap(dao -> dao.rules().stream())
                .filter(Objects::nonNull)
                .map(ruleId -> new RulesChangedEvent.Reference(ruleId, ruleId))
                .collect(Collectors.toSet());
        if (!updated.isEmpty()) {
            metadataUpdater.handleRuleChanges(new RulesChangedEvent(updated, Set.of()), state);
        }
    }

    public enum EventType {
        RULES_CHANGED,
        PIPELINES_CHANGED,
        PIPELINE_CONNECTIONS_CHANGED,
        INPUT_DELETED,
        INPUT_RENAMED
    }

    public static Config forRulesChanged(RulesChangedEvent event) {
        return Config.Builder.create().eventType(EventType.RULES_CHANGED).rulesEvent(event).build();
    }

    public static Config forPipelinesChanged(PipelinesChangedEvent event) {
        return Config.Builder.create().eventType(EventType.PIPELINES_CHANGED).pipelinesEvent(event).build();
    }

    public static Config forConnectionsChanged(PipelineConnectionsChangedEvent event) {
        return Config.Builder.create().eventType(EventType.PIPELINE_CONNECTIONS_CHANGED).connectionsEvent(event).build();
    }

    public static Config forInputDeleted(InputDeletedEvent event) {
        return Config.Builder.create().eventType(EventType.INPUT_DELETED).inputDeletedEvent(event).build();
    }

    public static Config forInputRenamed(InputRenamedEvent event) {
        return Config.Builder.create().eventType(EventType.INPUT_RENAMED).inputRenamedEvent(event).build();
    }

    @AutoValue
    @JsonDeserialize(builder = Config.Builder.class)
    @JsonTypeName(TYPE_NAME)
    public static abstract class Config implements SystemJobConfig {

        private static final String FIELD_EVENT_TYPE = "event_type";
        private static final String FIELD_RULES_EVENT = "rules_event";
        private static final String FIELD_PIPELINES_EVENT = "pipelines_event";
        private static final String FIELD_CONNECTIONS_EVENT = "connections_event";
        private static final String FIELD_INPUT_DELETED_EVENT = "input_deleted_event";
        private static final String FIELD_INPUT_RENAMED_EVENT = "input_renamed_event";

        @JsonProperty(FIELD_EVENT_TYPE)
        public abstract EventType eventType();

        @JsonProperty(FIELD_RULES_EVENT)
        @Nullable
        public abstract RulesChangedEvent rulesEvent();

        @JsonProperty(FIELD_PIPELINES_EVENT)
        @Nullable
        public abstract PipelinesChangedEvent pipelinesEvent();

        @JsonProperty(FIELD_CONNECTIONS_EVENT)
        @Nullable
        public abstract PipelineConnectionsChangedEvent connectionsEvent();

        @JsonProperty(FIELD_INPUT_DELETED_EVENT)
        @Nullable
        public abstract InputDeletedEvent inputDeletedEvent();

        @JsonProperty(FIELD_INPUT_RENAMED_EVENT)
        @Nullable
        public abstract InputRenamedEvent inputRenamedEvent();

        @Override
        public SystemJobInfo toInfo() {
            return SystemJobInfo.builder()
                    .type(type())
                    .description("Updates pipeline metadata in response to pipeline configuration changes.")
                    .statusInfo("Updating pipeline metadata for " + eventType() + ".")
                    .isCancelable(false)
                    .reportsProgress(false)
                    .build();
        }

        @AutoValue.Builder
        public static abstract class Builder implements SystemJobConfig.Builder<Builder> {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_PipelineMetadataUpdateJob_Config.Builder().type(TYPE_NAME);
            }

            @JsonProperty(FIELD_EVENT_TYPE)
            public abstract Builder eventType(EventType eventType);

            @JsonProperty(FIELD_RULES_EVENT)
            public abstract Builder rulesEvent(RulesChangedEvent event);

            @JsonProperty(FIELD_PIPELINES_EVENT)
            public abstract Builder pipelinesEvent(PipelinesChangedEvent event);

            @JsonProperty(FIELD_CONNECTIONS_EVENT)
            public abstract Builder connectionsEvent(PipelineConnectionsChangedEvent event);

            @JsonProperty(FIELD_INPUT_DELETED_EVENT)
            public abstract Builder inputDeletedEvent(InputDeletedEvent event);

            @JsonProperty(FIELD_INPUT_RENAMED_EVENT)
            public abstract Builder inputRenamedEvent(InputRenamedEvent event);

            public abstract Config build();
        }
    }
}
