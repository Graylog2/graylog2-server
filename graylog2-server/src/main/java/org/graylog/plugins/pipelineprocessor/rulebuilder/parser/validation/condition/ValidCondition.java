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
package org.graylog.plugins.pipelineprocessor.rulebuilder.parser.validation.condition;

import jakarta.inject.Inject;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderRegistry;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderStep;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragment;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.validation.ValidationResult;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.validation.Validator;

import java.util.Map;
import java.util.Objects;

public class ValidCondition implements Validator {

    protected final Map<String, RuleFragment> conditions;

    @Inject
    public ValidCondition(RuleBuilderRegistry ruleBuilderRegistry) {
        this.conditions = ruleBuilderRegistry.conditionsWithInternal();
    }

    @Override
    public ValidationResult validate(RuleBuilderStep step) {
        if (Objects.nonNull(step.function()) && !conditions.containsKey(step.function())) {
            return new ValidationResult(true, "Function " + step.function() + " not available as condition for rule builder.");
        }

        return new ValidationResult(false, "");
    }
}
