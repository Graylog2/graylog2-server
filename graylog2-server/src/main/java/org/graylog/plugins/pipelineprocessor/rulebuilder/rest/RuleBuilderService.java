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

import com.swrve.ratelimitedlogger.RateLimitedLog;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilder;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderStep;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter.getRateLimitedLog;

public class RuleBuilderService {

    private static final RateLimitedLog log = getRateLimitedLog(RuleBuilderService.class);

    private final String RULE_TEMPLATE = """
            rule \"%s\"
            when
              %s
            then
            %s
            end
            """;

    private final Map<String, Function<?>> conditions;
    private final Map<String, Function<?>> actions;

    @Inject
    public RuleBuilderService(FunctionRegistry functionRegistry) {
        this.conditions = functionRegistry.ruleBuilderConditions();
        this.actions = functionRegistry.ruleBuilderActions();
    }

    public String generateRuleSource(String title, RuleBuilder ruleBuilder, boolean generateSimulatorFields) {
        final String rule = String.format(RULE_TEMPLATE, title,
                generateConditions(ruleBuilder.conditions()),
                generateActions(ruleBuilder.actions(), generateSimulatorFields));
        log.debug(rule);
        return rule;
    }

    private String generateConditions(List<RuleBuilderStep> ruleConditions) {
        return "true" + System.lineSeparator() +
                ruleConditions.stream()
                        .map(step -> generateCondition(step))
                        .collect(Collectors.joining(System.lineSeparator()));
    }

    private String generateCondition(RuleBuilderStep step) {
        // TODO: error handling
        FunctionDescriptor<?> function = conditions.get(step.function()).descriptor();
        String syntax = "  && " + function.name() + "(" + System.lineSeparator();
        syntax += function.params().stream()
                .map(p -> addFunctionParameter(p, step))
                .filter(Objects::nonNull)
                .collect(Collectors.joining("," + System.lineSeparator()));
        return syntax + System.lineSeparator() + "  )";
    }


    private String generateActions(List<RuleBuilderStep> actions, boolean generateSimulatorFields) {
        return actions.stream().map(s -> generateAction(s, generateSimulatorFields)).collect(Collectors.joining(System.lineSeparator()));
    }

    private String generateAction(RuleBuilderStep step, boolean generateSimulatorFields) {
        FunctionDescriptor<?> function = actions.get(step.function()).descriptor();
        String syntax = "  ";
        if (Objects.nonNull(step.outputvariable())) {
            syntax += "let " + step.outputvariable() + " = ";
        }
        final String name = function.name();
        syntax += name + "(" + System.lineSeparator();
        syntax += function.params().stream().map(p -> addFunctionParameter(p, step))
                .filter(Objects::nonNull)
                .collect(Collectors.joining("," + System.lineSeparator()));
        syntax += System.lineSeparator() + "  );";
        if (generateSimulatorFields && Objects.nonNull(step.outputvariable())) {
            syntax += System.lineSeparator();
            syntax += "  set_field(\"gl2_simulator_" + step.outputvariable() + "\", " + step.outputvariable() + ");";
        }
        return syntax;
    }


    String addFunctionParameter(ParameterDescriptor descriptor, RuleBuilderStep step) {
        final String parameterName = descriptor.name();
        final Map<String, Object> parameters = step.parameters();
        if (Objects.isNull(parameters)) {
            return null;
        }
        final Object value = parameters.get(parameterName);
        String syntax = "    " + parameterName + " : ";
        if (value == null) {
            return null;
        } else if (value instanceof String valueString) {
            if (valueString.startsWith("$")) { // value set as variable
                syntax += ((String) value).substring(1);
            } else {
                syntax += "\"" + value + "\""; // value set as string
            }
        } else {
            syntax += value;
        }
        return syntax;
    }
}
