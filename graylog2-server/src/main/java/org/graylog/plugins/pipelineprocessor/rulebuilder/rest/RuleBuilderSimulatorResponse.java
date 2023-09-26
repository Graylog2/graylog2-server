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

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.graylog2.plugin.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RuleBuilderSimulatorResponse extends Message {

    private final static String VAR_CONDITION_PREFIX = "gl2_simulator_condition_";
    private final static Pattern OUTPUT_VARIABLE_PATTERN = Pattern.compile("^gl2_simulator_step_(\\d+)_(.*_\\d+)$");

    private final List<Pair<String, Object>> simulatorActionVariables;
    private Map<String, Object> simulatorConditionVariables;

    public RuleBuilderSimulatorResponse(Message simulatorResult) {
        super(simulatorResult.getFields());
        this.simulatorConditionVariables = new HashMap<>();
        this.simulatorActionVariables = new ArrayList<>(Collections.nCopies(simulatorResult.getFieldCount(), null));
        simulatorResult.getFields().entrySet().stream()
                .filter(e -> e.getKey().startsWith(VAR_CONDITION_PREFIX))
                .forEach(e -> {
                    this.simulatorConditionVariables.put(e.getKey().substring(VAR_CONDITION_PREFIX.length()), e.getValue());
                    this.removeField(e.getKey());
                });
        simulatorResult.getFields().entrySet().stream()
                .filter(e -> OUTPUT_VARIABLE_PATTERN.matcher(e.getKey()).matches())
                .forEach(e -> {
                    final Matcher matcher = OUTPUT_VARIABLE_PATTERN.matcher(e.getKey());
                    matcher.find();
                    int actionIndex = Integer.parseInt(matcher.group(1));
                    String variableName = matcher.group(2);
                    this.simulatorActionVariables.set(actionIndex, new ImmutablePair<>(variableName, e.getValue()));
                    this.removeField(e.getKey());
                });
        Iterables.removeIf(simulatorActionVariables, Predicates.isNull());
    }

    public List<Pair<String, Object>> getSimulatorActionVariables() {
        return simulatorActionVariables;
    }

    public Map<String, Object> getSimulatorConditionVariables() {
        return simulatorConditionVariables;
    }
}
