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
package org.graylog.plugins.pipelineprocessor.rulebuilder.rest;

import org.graylog2.plugin.Message;

import java.util.HashMap;
import java.util.Map;

public class RuleBuilderSimulatorResponse extends Message {

    private final static String VAR_CONDITION_PREFIX = "gl2_simulator_condition_";
    private final static String VAR_ACTION_PREFIX = "gl2_simulator_output_";

    private Map<String, Object> simulatorActionVariables;
    private Map<String, Object> simulatorConditionVariables;

    public RuleBuilderSimulatorResponse(Message simulatorResult) {
        super(simulatorResult.getFields());
        this.simulatorConditionVariables = new HashMap<>();
        this.simulatorActionVariables = new HashMap<>();
        simulatorResult.getFields().entrySet().stream()
                .filter(e -> e.getKey().startsWith(VAR_CONDITION_PREFIX))
                .forEach(e -> {
                    this.simulatorConditionVariables.put(e.getKey().substring(VAR_CONDITION_PREFIX.length()), e.getValue());
                    this.removeField(e.getKey());
                });
        simulatorResult.getFields().entrySet().stream()
                .filter(e -> e.getKey().startsWith(VAR_ACTION_PREFIX))
                .forEach(e -> {
                    this.simulatorActionVariables.put(e.getKey().substring(VAR_ACTION_PREFIX.length()), e.getValue());
                    this.removeField(e.getKey());
                });
    }

    public Map<String, Object> getSimulatorActionVariables() {
        return simulatorActionVariables;
    }

    public Map<String, Object> getSimulatorConditionVariables() {
        return simulatorConditionVariables;
    }
}
