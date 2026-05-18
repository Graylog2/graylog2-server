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

@AutoValue
public abstract class StageRuleLoad {
    @JsonProperty("rule_id")
    public abstract String ruleId();

    @JsonProperty("pipeline_id")
    public abstract String pipelineId();

    @JsonProperty("stage")
    public abstract int stage();

    @JsonProperty("load_percent")
    public abstract double loadPercent();

    public static StageRuleLoad create(String ruleId, String pipelineId, int stage, double loadPercent) {
        return new AutoValue_StageRuleLoad(ruleId, pipelineId, stage, loadPercent);
    }
}
