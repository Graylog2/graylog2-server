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
package org.graylog.plugins.pipelineprocessor.rulebuilder.parser.validation;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ValidatorService {

    private final Set<Validator> conditionValidators;
    private final Set<Validator> actionValidators;

    @Inject
    public ValidatorService(@Named("conditionValidators") final Set<Validator> conditionValidators, @Named("actionValidators")  Set<Validator> actionValidators) {
        this.conditionValidators = conditionValidators;
        this.actionValidators = actionValidators;
    }

    public List<ValidationResult> validate(RuleBuilder ruleBuilder) {
        List<ValidationResult> validationResults = new ArrayList<>();
        ruleBuilder.actions().forEach(ruleBuilderStep -> {
            for (Validator actionValidator : actionValidators) {
                validationResults.add(actionValidator.validate(ruleBuilderStep));
            }
        });

        ruleBuilder.conditions().forEach(ruleBuilderStep -> {
            for (Validator conditionValidator : conditionValidators) {
                validationResults.add(conditionValidator.validate(ruleBuilderStep));
            }
        });
        return validationResults;
    }
}
