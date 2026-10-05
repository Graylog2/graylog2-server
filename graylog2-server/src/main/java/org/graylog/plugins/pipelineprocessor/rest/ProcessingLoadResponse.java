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
package org.graylog.plugins.pipelineprocessor.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
public abstract class ProcessingLoadResponse {
    @JsonProperty("available")
    public abstract boolean available();

    @JsonProperty("total_cost_microseconds_per_second")
    public abstract double totalCostMicrosecondsPerSecond();

    @JsonProperty("stage_rules")
    public abstract List<StageRuleLoad> stageRules();

    @JsonProperty("pipelines")
    public abstract List<PipelineLoad> pipelines();

    @JsonProperty("rules")
    public abstract List<RuleLoad> rules();

    public static ProcessingLoadResponse create(boolean available,
                                                double totalCostMicrosecondsPerSecond,
                                                List<StageRuleLoad> stageRules,
                                                List<PipelineLoad> pipelines,
                                                List<RuleLoad> rules) {
        return new AutoValue_ProcessingLoadResponse(available, totalCostMicrosecondsPerSecond, stageRules, pipelines, rules);
    }

    static ProcessingLoadResponse unavailable() {
        return create(false, 0.0d, List.of(), List.of(), List.of());
    }
}
