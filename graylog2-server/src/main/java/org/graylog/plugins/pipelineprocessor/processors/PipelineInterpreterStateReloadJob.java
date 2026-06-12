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
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.inject.assistedinject.AssistedInject;
import com.swrve.ratelimitedlogger.RateLimitedLog;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.scheduler.JobExecutionException;
import org.graylog.scheduler.system.SystemJob;
import org.graylog.scheduler.system.SystemJobConfig;
import org.graylog.scheduler.system.SystemJobContext;
import org.graylog.scheduler.system.SystemJobInfo;
import org.graylog.scheduler.system.SystemJobResult;

import java.time.Duration;

import static org.graylog2.plugin.utilities.ratelimitedlog.RateLimitedLogFactory.createDefaultRateLimitedLog;

/**
 * System scheduler job that reloads the pipeline interpreter state. Submitted by
 * {@link PipelineInterpreterStateUpdater} in response to pipeline-related server events.
 * On transient failures (e.g. MongoDB hiccups), the job retries with a 1-second delay.
 */
public class PipelineInterpreterStateReloadJob implements SystemJob<PipelineInterpreterStateReloadJob.Config> {

    public static final String TYPE_NAME = "pipeline-interpreter-state-reload";

    private static final RateLimitedLog log = createDefaultRateLimitedLog(PipelineInterpreterStateReloadJob.class);

    private final PipelineInterpreterStateBuilder stateBuilder;
    private final PipelineInterpreterStateUpdater stateUpdater;
    private final PipelineMetricRegistry pipelineMetricRegistry;

    public interface Factory extends SystemJob.Factory<PipelineInterpreterStateReloadJob> {
        @Override
        PipelineInterpreterStateReloadJob create();
    }

    @AssistedInject
    public PipelineInterpreterStateReloadJob(PipelineInterpreterStateBuilder stateBuilder,
                                             PipelineInterpreterStateUpdater stateUpdater,
                                             MetricRegistry metricRegistry) {
        this.stateBuilder = stateBuilder;
        this.stateUpdater = stateUpdater;
        this.pipelineMetricRegistry = PipelineMetricRegistry.create(metricRegistry, Pipeline.class.getName(), Rule.class.getName());
    }

    @Override
    public SystemJobResult execute(Config config, SystemJobContext ctx) throws JobExecutionException {
        try {
            final PipelineInterpreter.State newState = stateBuilder.buildState(pipelineMetricRegistry);
            stateUpdater.updateState(newState);
            return SystemJobResult.success();
        } catch (Exception e) {
            log.warn("Failed to reload pipeline interpreter state, retrying: {}", e.getMessage());
            return SystemJobResult.withRetry(Duration.ofSeconds(1), Integer.MAX_VALUE);
        }
    }

    public static Config create() {
        return Config.Builder.create().build();
    }

    @AutoValue
    @JsonDeserialize(builder = Config.Builder.class)
    @JsonTypeName(TYPE_NAME)
    public static abstract class Config implements SystemJobConfig {

        @Override
        public SystemJobInfo toInfo() {
            return SystemJobInfo.builder()
                    .type(type())
                    .description("Reloads the pipeline interpreter state from MongoDB.")
                    .statusInfo("Reloading pipeline interpreter state.")
                    .isCancelable(false)
                    .reportsProgress(false)
                    .build();
        }

        @AutoValue.Builder
        public static abstract class Builder implements SystemJobConfig.Builder<Builder> {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_PipelineInterpreterStateReloadJob_Config.Builder().type(TYPE_NAME);
            }

            public abstract Config build();
        }
    }
}
