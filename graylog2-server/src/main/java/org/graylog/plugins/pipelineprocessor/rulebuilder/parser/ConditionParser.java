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
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderStep;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ConditionParser {

    public static final String NL = System.lineSeparator();
    protected final Map<String, Function<?>> conditions;

    @Inject
    public ConditionParser(FunctionRegistry functionRegistry) {
        this.conditions = functionRegistry.ruleBuilderConditions();
    }


    public String generate(List<RuleBuilderStep> ruleConditions) {
        return "  true" + NL +
                ruleConditions.stream()
                        .map(step -> generateCondition(step))
                        .collect(Collectors.joining(NL));
    }

    String generateCondition(RuleBuilderStep step) {
        if (!conditions.containsKey(step.function())) {
            throw new IllegalArgumentException("Function " + step.function() + " not available as condition for rule builder.");
        }

        FunctionDescriptor<?> function = conditions.get(step.function()).descriptor();
        String syntax = "  && ";
        if (step.negate()) {
            syntax += "! ";
        }
        syntax += function.name() + "(";
        String params = function.params().stream()
                .map(p -> ParserUtil.addFunctionParameter(p, step))
                .filter(Objects::nonNull)
                .collect(Collectors.joining("," + NL));
        if (StringUtils.isEmpty(params)) {
            return syntax + ")";
        } else {
            return syntax + NL + params + NL + "  )";
        }
    }

}
