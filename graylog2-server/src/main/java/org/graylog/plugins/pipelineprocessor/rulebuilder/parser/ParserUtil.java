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
package org.graylog.plugins.pipelineprocessor.rulebuilder.parser;

import org.apache.commons.lang3.StringUtils;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderStep;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragment;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ParserUtil {


    static final String generateForFunction(RuleBuilderStep step, FunctionDescriptor<?> function) {
        String syntax = function.name() + "(";
        String params = function.params().stream()
                .map(p -> addFunctionParameter(p, step))
                .filter(Objects::nonNull)
                .collect(Collectors.joining("," + ConditionParser.NL));
        if (StringUtils.isEmpty(params)) {
            return syntax + ")";
        } else {
            return syntax + ConditionParser.NL + params + ConditionParser.NL + "  )";
        }
    }

    static final String addFunctionParameter(ParameterDescriptor descriptor, RuleBuilderStep step) {
        final String parameterName = descriptor.name(); // parameter name needed by function
        final Map<String, Object> parameters = step.parameters();
        if (Objects.isNull(parameters)) {
            return null;
        }
        final Object value = parameters.get(parameterName); // parameter value set by rule definition
        String syntax = "    " + parameterName + " : ";
        if ((value == null) && !descriptor.optional()) {
            throw new IllegalArgumentException("Required parameter " + parameterName + "not set.");
        } else if (value == null) {
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

    static final String generateForFragment(RuleBuilderStep step, RuleFragment ruleFragment) {
        String fragment = ruleFragment.fragment();

        for (Object param : ruleFragment.descriptor().params()) {
            fragment = addFragmentParameter(fragment, (ParameterDescriptor) param, step);
        }

        if (StringUtils.contains(fragment, "{")) {
            throw new IllegalArgumentException("Could not replace all fragment parameters");
        }

        return fragment;
    }

    static String addFragmentParameter(String fragment, ParameterDescriptor descriptor, RuleBuilderStep step) {
        final String parameterName = descriptor.name(); // parameter name needed by function
        final Map<String, Object> parameters = step.parameters();
        if (Objects.isNull(parameters)) {
            throw new IllegalArgumentException("Cannot replace " + parameterName + " because no parameter values are provided.");
        }
        final Object value = parameters.get(parameterName); // parameter value set by rule definition
        if (value == null) {
            throw new IllegalArgumentException("Required parameter " + parameterName + " not set.");
        }
        String replaced = StringUtils.replace(fragment, "{" + parameterName + "}", value.toString());
        if (StringUtils.equals(replaced, fragment)) {
            throw new IllegalArgumentException("Parameter " + parameterName + " not set in fragment");
        }
        return replaced;
    }
}
