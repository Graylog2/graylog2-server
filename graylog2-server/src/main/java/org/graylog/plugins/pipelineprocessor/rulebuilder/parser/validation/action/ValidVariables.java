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
package org.graylog.plugins.pipelineprocessor.rulebuilder.parser.validation.action;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderRegistry;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderStep;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragment;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.validation.ValidationResult;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.validation.Validator;

import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.graylog2.shared.utilities.StringUtils.f;

public class ValidVariables implements Validator {

    private final Map<String, RuleFragment> actions;
    private Map<String, Class<?>> variables;

    @Inject
    public ValidVariables(RuleBuilderRegistry ruleBuilderRegistry) {
        this.actions = ruleBuilderRegistry.actions();
        this.variables = new HashMap<>();
    }

    @Override
    public ValidationResult validate(RuleBuilderStep step) {
        final RuleFragment ruleFragment = actions.get(step.function());
        FunctionDescriptor<?> functionDescriptor = ruleFragment.descriptor();
        Map<String, Object> stepParameters = step.parameters();

        //Add output to map
        String outputvariable = step.outputvariable();
        if (StringUtils.isNotBlank(outputvariable)) {

            if (functionDescriptor.returnType() == Void.class) {
                return new ValidationResult(true, f("Return type is void. No output variable allowed", functionDescriptor.name()));
            }

            storeVariable(outputvariable, functionDescriptor.returnType());
        }

        ImmutableList<ParameterDescriptor> parameterDescriptors = functionDescriptor.params();
        for (ParameterDescriptor parameterDescriptor : parameterDescriptors) {
            String parameterName = parameterDescriptor.name();
            Object value = stepParameters.get(parameterName);
            Class<?> variableType = getVariableType(value);

            if (!parameterDescriptor.optional() && value == null) {
                return new ValidationResult(true, f("Missing parameter %s", parameterName));
            }

            //$ means it is stored in another variable and we need to fetch and verify that type
            if (value instanceof String s && s.startsWith("$")) {
                String substring = s.substring(1);
                Class<?> passedVariableType = variables.get(substring);
                if (Objects.isNull(passedVariableType)) {
                    return new ValidationResult(true, f("Could not find passed variable %s", value));
                }
                variableType = passedVariableType;
            }

            //Check if variable type matches function expectation
            Class<?> paramType = parameterDescriptor.type();
            if (value != null && paramType != Object.class && variableType != paramType) {
                String errorMsg = "Found a wrong parameter type for parameter %s";
                return new ValidationResult(true, f(errorMsg, parameterName));
            }
        }

        return new ValidationResult(false, "");
    }

    private void storeVariable(String name, Class<?> type) {
        variables.put(name, type);
    }

    private Class<?> getVariableType(Object type) {
        if (type instanceof Double) {
            return Double.class;
        } else if (type instanceof Integer) {
            return Long.class;
        } else if (type instanceof Long) {
            return Long.class;
        } else if (type instanceof String) {
            return String.class;
        } else if (type instanceof Boolean) {
            return Boolean.class;
        } else {
            return Object.class;
        }
    }
}
