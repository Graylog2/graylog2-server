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

import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderStep;

import java.util.Map;
import java.util.Objects;

public class ParserUtil {

    protected static final String addFunctionParameter(ParameterDescriptor descriptor, RuleBuilderStep step) {
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

}
