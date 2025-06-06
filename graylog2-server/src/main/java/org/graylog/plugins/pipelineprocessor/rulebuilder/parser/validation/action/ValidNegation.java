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

import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderRegistry;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderStep;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragment;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.validation.ValidationResult;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.validation.Validator;

import java.util.Map;

public class ValidNegation implements Validator {
    private final Map<String, RuleFragment> actions;

    public ValidNegation(RuleBuilderRegistry ruleBuilderRegistry) {
        this.actions = ruleBuilderRegistry.actionsWithInternal();
    }

    @Override
    public ValidationResult validate(RuleBuilderStep step) {
        final RuleFragment ruleFragment = actions.get(step.function());

        if (step.negate()) {
            if (ruleFragment.isFragment()) {
                return new ValidationResult(true, "Negation of fragments not possible ");
            } else {
                FunctionDescriptor<?> function = ruleFragment.descriptor();
                if (!function.returnType().equals(Boolean.class)) {
                    return new ValidationResult(true, "None boolean function " + step.function() + " cannot be negated.");
                }
            }
        }
        return new ValidationResult(false, "");
    }
}
