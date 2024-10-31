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

import io.jsonwebtoken.lang.Collections;
import org.graylog.plugins.pipelineprocessor.parser.ParseException;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilder;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderStep;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.RuleBuilderService;
import org.graylog.plugins.pipelineprocessor.rulebuilder.rest.RuleBuilderDto;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ValidatorService {

    private final Set<Validator> conditionValidators;
    private final Set<Validator> actionValidators;
    private final PipelineRuleParser pipelineRuleParser;
    private final RuleBuilderService ruleBuilderService;

    @Inject
    public ValidatorService(@Named("conditionValidators") final Set<Validator> conditionValidators, @Named("actionValidators") Set<Validator> actionValidators, PipelineRuleParser pipelineRuleParser, RuleBuilderService ruleBuilderService) {
        this.conditionValidators = conditionValidators;
        this.actionValidators = actionValidators;
        this.pipelineRuleParser = pipelineRuleParser;
        this.ruleBuilderService = ruleBuilderService;
    }

    public RuleBuilderDto validate(RuleBuilderDto ruleBuilderDto) {
        RuleBuilder ruleBuilder = ruleBuilderDto.ruleBuilder();
        RuleBuilder.Builder validationBuilder = ruleBuilder.toBuilder();

        List<RuleBuilderStep> validatedActions = validateWithResults(ruleBuilder.actions(), actionValidators);
        validationBuilder.actions(validatedActions);

        List<RuleBuilderStep> validatedConditions = validateWithResults(ruleBuilder.conditions(), conditionValidators);
        validationBuilder.conditions(validatedConditions);


        String source = null;
        try {
            validationBuilder.errors(null);
            source = parseRule(ruleBuilderDto, validationBuilder.build());
        } catch (Exception exception) {
            if (validatedConditions.stream().allMatch(step -> Collections.isEmpty(step.errors()))
                    && validatedActions.stream().allMatch(step -> Collections.isEmpty(step.errors()))) {
                validationBuilder.errors(List.of(exception.getMessage()));
            }
        }

        RuleBuilder validatedRuleBuilder = validationBuilder.build();
        return ruleBuilderDto.toBuilder().ruleBuilder(validatedRuleBuilder).source(source).build();
    }

    private String parseRule(RuleBuilderDto ruleBuilderDto, RuleBuilder validatedRuleBuilder) throws ParseException {
        String source = ruleBuilderService.generateRuleSource(ruleBuilderDto.title(), validatedRuleBuilder, false);
        pipelineRuleParser.parseRule(source, true);
        return source;
    }

    public void validateAndFailFast(RuleBuilderDto ruleBuilderDto) throws IllegalArgumentException, ParseException {
        RuleBuilder ruleBuilder = ruleBuilderDto.ruleBuilder();
        validateAndFail(ruleBuilder.actions(), actionValidators);
        validateAndFail(ruleBuilder.conditions(), conditionValidators);
        parseRule(ruleBuilderDto, ruleBuilder);
    }

    private void validateAndFail(List<RuleBuilderStep> steps, Set<Validator> validators) {
        if (Objects.nonNull(steps)) {
            steps.forEach(ruleBuilderStep -> {
                for (Validator actionValidator : validators) {
                    ValidationResult result = actionValidator.validate(ruleBuilderStep);
                    if (result.failed()) {
                        throw new IllegalArgumentException("Validation failed: " + result.failureReason());
                    }
                }
            });
        }
    }

    private List<RuleBuilderStep> validateWithResults(List<RuleBuilderStep> actionSteps, Set<Validator> validators) {
        List<RuleBuilderStep> validatedSteps = new ArrayList<>();
        if (Objects.nonNull(actionSteps)) {
            actionSteps.forEach(ruleBuilderStep -> {
                List<String> errors = new ArrayList<>();
                for (Validator actionValidator : validators) {
                    ValidationResult result = actionValidator.validate(ruleBuilderStep);

                    if (result.failed()) {
                        errors.add(result.failureReason());
                    }

                }
                ruleBuilderStep = ruleBuilderStep.toBuilder().errors(errors).build();
                validatedSteps.add(ruleBuilderStep);
            });
        }

        return validatedSteps;
    }
}
