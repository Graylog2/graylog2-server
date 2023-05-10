/*
 *  Copyright (C) 2020 Graylog, Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Server Side Public License, version 1,
 *  as published by MongoDB, Inc.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  Server Side Public License for more details.
 *
 *  You should have received a copy of the Server Side Public License
 *  along with this program. If not, see
 *  <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.pipelineprocessor.rulebuilder.parser;

import org.apache.commons.lang3.StringUtils;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderRegistry;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderStep;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ActionParser {

    public static final String NL = System.lineSeparator();
    protected final Map<String, Function<?>> actions;

    @Inject
    public ActionParser(RuleBuilderRegistry ruleBuilderRegistry) {
        this.actions = ruleBuilderRegistry.actions();
    }

    public String generate(List<RuleBuilderStep> actions, boolean generateSimulatorFields) {
        return actions.stream().map(s -> generateAction(s, generateSimulatorFields)).collect(Collectors.joining(NL));
    }

    String generateAction(RuleBuilderStep step, boolean generateSimulatorFields) {
        if (!actions.containsKey(step.function())) {
            throw new IllegalArgumentException("Function " + step.function() + " not available as action for rule builder.");
        }

        FunctionDescriptor<?> function = actions.get(step.function()).descriptor();
        String syntax = "  ";
        if (Objects.nonNull(step.outputvariable())) {
            if (function.returnType().equals(Void.class)) {
                throw new IllegalArgumentException("Function " + step.function() + " does not return a value.");
            }
            syntax += "let " + step.outputvariable() + " = ";
        }

        if (step.negate()) {
            if (!function.returnType().equals(Boolean.class)) {
                throw new IllegalArgumentException("Function " + step.function() + " cannot be negated.");
            }
            syntax += "! ";
        }

        syntax += function.name() + "(";
        String params = function.params().stream().map(p -> ParserUtil.addFunctionParameter(p, step))
                .filter(Objects::nonNull)
                .collect(Collectors.joining("," + NL));
        if (StringUtils.isEmpty(params)) {
            syntax += ");";
        } else {
            syntax += NL + params + NL + "  );";
        }

        // generate message fields for simulator
        if (generateSimulatorFields && Objects.nonNull(step.outputvariable())) {
            syntax += NL;
            syntax += "  set_field(\"gl2_simulator_" + step.outputvariable() + "\", " + step.outputvariable() + ");";
        }

        return syntax;
    }

}
