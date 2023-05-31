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

import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.functions.messages.SetField;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderRegistry;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderStep;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragment;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.validation.ValidationResult;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.validation.Validator;

import java.util.Map;

public class ValidNewMessageField implements Validator {
    private final Map<String, RuleFragment> actions;

    @Inject
    public ValidNewMessageField(RuleBuilderRegistry ruleBuilderRegistry) {
        this.actions = ruleBuilderRegistry.actions();
    }

    @Override
    public ValidationResult validate(RuleBuilderStep step) {
        final RuleFragment ruleFragment = actions.get(step.function());
        FunctionDescriptor<?> functionDescriptor = ruleFragment.descriptor();

        if (functionDescriptor.name().equals(SetField.NAME)) {
            Object value = step.parameters().get("field");
            return validate((String) value);
        }
        return new ValidationResult(false, "");
    }

    //Todo: GIM conform
    private ValidationResult validate(String value) {
        if (StringUtils.containsWhitespace(value)) {
            return new ValidationResult(true, "New field names must be underscore seperated. Blank spaces are not allowed!");
        }

        return new ValidationResult(false, "");
    }
}
