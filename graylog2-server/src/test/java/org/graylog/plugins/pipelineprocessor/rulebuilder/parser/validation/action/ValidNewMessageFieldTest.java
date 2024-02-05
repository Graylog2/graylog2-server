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
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.functions.messages.SetField;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderRegistry;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderStep;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragment;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.FunctionUtil;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidNewMessageFieldTest {

    public static final String FIELD_PARAM = "field";
    public static final String FIELDS_PARAM = "fields";
    public static final String WITH_SPACES = "with spaces";
    @Mock
    RuleBuilderRegistry ruleBuilderRegistry;

    ValidNewMessageField classUnderTest;

    @BeforeEach
    void setUp() {
        Map<String, RuleFragment> actions = new HashMap<>();
        Function<Boolean> stringFunction = FunctionUtil.testFunction(ValidVariablesTest.STRING_FUNCTION, ImmutableList.of(
                string(FIELD_PARAM).optional().build()
        ), Boolean.class);
        actions.put(ValidVariablesTest.STRING_FUNCTION, RuleFragment.builder()
                .descriptor(stringFunction.descriptor())
                .build());

        Function<Boolean> setFieldFunction = FunctionUtil.testFunction(SetField.NAME, ImmutableList.of(
                string(FIELD_PARAM).optional().build()
        ), Boolean.class);
        actions.put(SetField.NAME, RuleFragment.builder()
                .descriptor(setFieldFunction.descriptor())
                .build());

        when(ruleBuilderRegistry.actions()).thenReturn(actions);

        classUnderTest = new ValidNewMessageField(ruleBuilderRegistry);
    }

    @Test
    void validateSetFieldFunctionFailsWithSpaces() {
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put(FIELD_PARAM, WITH_SPACES);
        RuleBuilderStep invalidStep = RuleBuilderStep.builder()
                .parameters(parameters)
                .function(SetField.NAME)
                .build();

        ValidationResult result = classUnderTest.validate(invalidStep);

        assertThat(result.failed()).isTrue();
    }

    @Test
    void validateSetFieldFunction() {
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put(FIELD_PARAM, "valid_new_field");
        RuleBuilderStep validStep = RuleBuilderStep.builder()
                .parameters(parameters)
                .function(SetField.NAME)
                .build();

        ValidationResult result = classUnderTest.validate(validStep);

        assertThat(result.failed()).isFalse();
    }

    @Test
    void validateOtherFunctionsAreSkipped() {
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put(FIELD_PARAM, WITH_SPACES);
        RuleBuilderStep randomOtherFunction = RuleBuilderStep
                .builder()
                .parameters(parameters)
                .function(ValidVariablesTest.STRING_FUNCTION)
                .build();

        ValidationResult result = classUnderTest.validate(randomOtherFunction);

        assertThat(result.failed()).isFalse();
    }

}
