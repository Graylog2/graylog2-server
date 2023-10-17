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
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.integer;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidVariablesTest {

    public static final String STRING_FUNCTION = "stringFunction";
    public static final String INTEGER_FUNCTION = "integerFunction";
    public static final String VOID_FUNCTION = "voidFunction";
    public static final String INT_PARAM = "intParam";
    public static final String STRING_PARAM = "stringParam";
    public static final String VOID_PARAM = "voidParam";
    @Mock
    RuleBuilderRegistry ruleBuilderRegistry;

    ValidVariables classUnderTest;

    @BeforeEach
    void setUp() {
        Map<String, RuleFragment> actions = new HashMap<>();
        Function<Boolean> integerFunction = FunctionUtil.testFunction(INTEGER_FUNCTION, ImmutableList.of(
                integer(INT_PARAM).build(), integer("optionalInt").optional().build()
        ), Boolean.class);
        Function<Boolean> stringFunction = FunctionUtil.testFunction(STRING_FUNCTION, ImmutableList.of(
                string(STRING_PARAM).build(), string("optionalString").optional().build()
        ), String.class);
        Function<Boolean> voidFunction = FunctionUtil.testFunction(VOID_FUNCTION, ImmutableList.of(
                string(VOID_PARAM).optional().build()
        ), Void.class);

        actions.put(STRING_FUNCTION, RuleFragment.builder().descriptor(stringFunction.descriptor()).build());
        actions.put(INTEGER_FUNCTION, RuleFragment.builder().descriptor(integerFunction.descriptor()).build());
        actions.put(VOID_FUNCTION, RuleFragment.builder().descriptor(voidFunction.descriptor()).build());

        when(ruleBuilderRegistry.actions()).thenReturn(actions);

        classUnderTest = new ValidVariables(ruleBuilderRegistry);
    }

    @Test
    void failsWhenRequiredParamMissing() {
        RuleBuilderStep stepWithValidNegation = RuleBuilderStep.builder().parameters(new HashMap<>()).function(INTEGER_FUNCTION).build();
        ValidationResult result = classUnderTest.validate(stepWithValidNegation);

        assertThat(result.failed()).isTrue();
        assertThat(result.failureReason()).isEqualTo("Missing parameter " + INT_PARAM);
    }

    @Test
    void failsWhenPassedParamMissing() {
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put(INT_PARAM, "$fromOutput");
        RuleBuilderStep stepWithValidNegation = RuleBuilderStep.builder().parameters(parameters).function(INTEGER_FUNCTION).build();
        ValidationResult result = classUnderTest.validate(stepWithValidNegation);

        assertThat(result.failed()).isTrue();
        assertThat(result.failureReason()).isEqualTo("Could not find passed variable $fromOutput");
    }

    @Test
    void failsWhenReturnExpectedFromVoid() {
        RuleBuilderStep stepWithValidNegation = RuleBuilderStep.builder()
                .outputvariable("voidOutput")
                .parameters(new HashMap<>())
                .function(VOID_FUNCTION)
                .build();
        ValidationResult result = classUnderTest.validate(stepWithValidNegation);

        assertThat(result.failed()).isTrue();
        assertThat(result.failureReason()).isEqualTo("Return type is void. No output variable allowed");
    }

    @Test
    void failsWhenPassedWrongTypeParam() {
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put(INT_PARAM, "1");
        RuleBuilderStep stepWithValidNegation = RuleBuilderStep.builder()
                .parameters(parameters)
                .function(INTEGER_FUNCTION)
                .build();
        ValidationResult result = classUnderTest.validate(stepWithValidNegation);

        assertThat(result.failed()).isTrue();
        assertThat(result.failureReason()).isEqualTo("Found a wrong parameter type for parameter " + INT_PARAM);
    }

    @Test
    void failsWhenPassedWrongTypeParamFromOutPut() {

        HashMap<String, Object> stringFunctionParams = new HashMap<>();
        stringFunctionParams.put(STRING_PARAM, "test string");
        RuleBuilderStep validStringStep = RuleBuilderStep.builder()
                .parameters(stringFunctionParams)
                .function(STRING_FUNCTION)
                .outputvariable("intOutPut")
                .build();
        ValidationResult firstFunctionResult = classUnderTest.validate(validStringStep);
        assertThat(firstFunctionResult.failed()).isFalse();

        HashMap<String, Object> intFunctionParams = new HashMap<>();
        intFunctionParams.put(INT_PARAM, "$intOutPut");
        RuleBuilderStep stepWithWrongTypPassedOutput = RuleBuilderStep.builder()
                .parameters(intFunctionParams)
                .function(INTEGER_FUNCTION)
                .build();
        ValidationResult result = classUnderTest.validate(stepWithWrongTypPassedOutput);

        assertThat(result.failed()).isTrue();
        assertThat(result.failureReason()).contains("Found a wrong parameter type for parameter " + INT_PARAM);
    }

    @Test
    void validateSuccessFull() {

        HashMap<String, Object> stringFunctionParams = new HashMap<>();
        stringFunctionParams.put(STRING_PARAM, "test string");
        RuleBuilderStep validStringStep = RuleBuilderStep.builder()
                .parameters(stringFunctionParams)
                .function(STRING_FUNCTION)
                .outputvariable("stringOutPut")
                .build();
        ValidationResult firstFunctionResult = classUnderTest.validate(validStringStep);

        assertThat(firstFunctionResult.failed()).isFalse();

        HashMap<String, Object> intFunctionParams = new HashMap<>();
        intFunctionParams.put(STRING_PARAM, "stringOutPut");
        RuleBuilderStep stepWithCorrectPassedOutput = RuleBuilderStep.builder()
                .parameters(intFunctionParams)
                .function(STRING_FUNCTION)
                .build();
        ValidationResult result = classUnderTest.validate(stepWithCorrectPassedOutput);

        assertThat(result.failed()).isFalse();
    }
}
