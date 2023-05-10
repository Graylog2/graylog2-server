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

import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderStep;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ActionParser {

    protected final Map<String, Function<?>> actions;

    @Inject
    public ActionParser(FunctionRegistry functionRegistry) {
        this.actions = functionRegistry.ruleBuilderActions();
    }

    public String generate(List<RuleBuilderStep> actions, boolean generateSimulatorFields) {
        return actions.stream().map(s -> generateAction(s, generateSimulatorFields)).collect(Collectors.joining(System.lineSeparator()));
    }

    String generateAction(RuleBuilderStep step, boolean generateSimulatorFields) {
        FunctionDescriptor<?> function = actions.get(step.function()).descriptor();
        String syntax = "  ";
        if (Objects.nonNull(step.outputvariable())) {
            syntax += "let " + step.outputvariable() + " = ";
        }
        final String name = function.name();
        syntax += name + "(" + System.lineSeparator();
        syntax += function.params().stream().map(p -> ParserUtil.addFunctionParameter(p, step))
                .filter(Objects::nonNull)
                .collect(Collectors.joining("," + System.lineSeparator()));
        syntax += System.lineSeparator() + "  );";
        if (generateSimulatorFields && Objects.nonNull(step.outputvariable())) {
            syntax += System.lineSeparator();
            syntax += "  set_field(\"gl2_simulator_" + step.outputvariable() + "\", " + step.outputvariable() + ");";
        }
        return syntax;
    }

}
