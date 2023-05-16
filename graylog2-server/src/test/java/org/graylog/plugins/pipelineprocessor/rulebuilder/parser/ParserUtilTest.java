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

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderStep;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragment;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.integer;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ParserUtilTest {


    private static final String NL = System.lineSeparator();

    @Test
    public void generateFunctionWithNoParamGeneration() {
        RuleBuilderStep step = RuleBuilderStep.builder().function("function1").build();
        final FunctionDescriptor<Boolean> descriptor = FunctionUtil.testFunction(
                "function1", ImmutableList.of(
                        string("required").build(),
                        integer("optional").optional().build()
                ), Boolean.class
        ).descriptor();
        assertThat(ParserUtil.generateForFunction(step, descriptor)).isEqualTo(
                "function1()"
        );
    }

    @Test
    public void generateFunctionWithSingleParamGeneration() {
        RuleBuilderStep step = RuleBuilderStep.builder().function("function1")
                .parameters(Map.of("required", "val1")).build();
        final FunctionDescriptor<Boolean> descriptor = FunctionUtil.testFunction(
                "function1", ImmutableList.of(
                        string("required").build(),
                        integer("optional").optional().build()
                ), Boolean.class
        ).descriptor();
        assertThat(ParserUtil.generateForFunction(step, descriptor)).isEqualTo(
                "function1(" + NL + "    required : \"val1\"" + NL + "  )"
        );
    }

    @Test
    public void generateFunctionWithMultipleParamsGeneration() {
        RuleBuilderStep step = RuleBuilderStep.builder().function("function1")
                .parameters(Map.of("required", "val1", "optional", 1)).build();
        final FunctionDescriptor<Boolean> descriptor = FunctionUtil.testFunction(
                "function1", ImmutableList.of(
                        string("required").build(),
                        integer("optional").optional().build()
                ), Boolean.class
        ).descriptor();
        assertThat(ParserUtil.generateForFunction(step, descriptor)).isEqualTo(
                "function1(" + NL +
                        "    required : \"val1\"," + NL +
                        "    optional : 1" + NL +
                        "  )"
        );
    }


    @Test
    public void addFunctionParameterNull_WhenNoParametersAreSet() {
        RuleBuilderStep step = mock(RuleBuilderStep.class);
        when(step.parameters()).thenReturn(null);
        ParameterDescriptor descriptor = mock(ParameterDescriptor.class);
        assertThat(ParserUtil.addFunctionParameter(descriptor, step)).isNull();
    }

    @Test
    public void throwException_WhenNoParameterValueForRequiredParamIsSet() {
        RuleBuilderStep step = mock(RuleBuilderStep.class);
        Map<String, Object> params = new HashMap<>();
        when(step.parameters()).thenReturn(params);
        ParameterDescriptor descriptor = mock(ParameterDescriptor.class);
        when(descriptor.optional()).thenReturn(false);
        assertThatThrownBy(() -> ParserUtil.addFunctionParameter(descriptor, step)).isInstanceOf(IllegalArgumentException.class);
    }


    @Test
    public void addFunctionParameterNull_WhenNoParameterValueIsSet() {
        RuleBuilderStep step = mock(RuleBuilderStep.class);
        Map<String, Object> params = new HashMap<>();
        when(step.parameters()).thenReturn(params);
        ParameterDescriptor descriptor = mock(ParameterDescriptor.class);
        when(descriptor.optional()).thenReturn(true);
        assertThat(ParserUtil.addFunctionParameter(descriptor, step)).isNull();
    }

    @Test
    public void addFunctionParameterSyntaxOk_WhenStringParameterValueIsSet() {
        String parameterName = "foo";
        var parameterValue = "bar";
        RuleBuilderStep step = mock(RuleBuilderStep.class);
        Map<String, Object> params = Map.of(parameterName, parameterValue);
        when(step.parameters()).thenReturn(params);
        ParameterDescriptor descriptor = mock(ParameterDescriptor.class);
        when(descriptor.name()).thenReturn(parameterName);
        assertThat(ParserUtil.addFunctionParameter(descriptor, step))
                .isEqualTo("    foo : \"bar\"");
    }

    @Test
    public void addFunctionParameterSyntaxOk_WhenNumericParameterValueIsSet() {
        String parameterName = "foo";
        var parameterValue = 42;
        RuleBuilderStep step = mock(RuleBuilderStep.class);
        Map<String, Object> params = Map.of(parameterName, parameterValue);
        when(step.parameters()).thenReturn(params);
        ParameterDescriptor descriptor = mock(ParameterDescriptor.class);
        when(descriptor.name()).thenReturn(parameterName);
        assertThat(ParserUtil.addFunctionParameter(descriptor, step))
                .isEqualTo("    foo : 42");
    }

    @Test
    public void addFunctionParameterSyntaxOk_WhenVariableParameterValueIsSet() {
        String parameterName = "foo";
        String parameterValue = "$bar";
        RuleBuilderStep step = mock(RuleBuilderStep.class);
        Map<String, Object> params = Map.of(parameterName, parameterValue);
        when(step.parameters()).thenReturn(params);
        ParameterDescriptor descriptor = mock(ParameterDescriptor.class);
        when(descriptor.name()).thenReturn(parameterName);
        assertThat(ParserUtil.addFunctionParameter(descriptor, step))
                .isEqualTo("    foo : bar");
    }

    // Fragment Building

    static final String fragment = "( hasField(\"{field}\") and to_string($message.{field})==\"{fieldValue}\" )";
    static RuleFragment ruleFragment = RuleFragment.builder()
            .fragment(fragment)
            .descriptor(FunctionDescriptor.builder()
                    .name("testfunction")
                    .params(ImmutableList.of(
                            string("field").build(),
                            string("fieldValue").build()
                    ))
                    .returnType(Boolean.class)
                    .build())
            .build();

    @Test
    public void generateForFragmentWithoutParams() {
        RuleFragment verySimpleFragment = RuleFragment.builder()
                .fragment("true")
                .descriptor(FunctionDescriptor.builder()
                        .name("simpleFragment")
                        .params(ImmutableList.of())
                        .returnType(Boolean.class)
                        .build())
                .build();
        RuleBuilderStep step = mock(RuleBuilderStep.class);
        assertThat(ParserUtil.generateForFragment(step, verySimpleFragment))
                .isEqualTo("true");
    }

    @Test
    public void generateForFragmentThrowsException_WhenNotAllParametersAreSet() {
        RuleFragment incompleteFragment = RuleFragment.builder()
                .fragment(fragment)
                .descriptor(FunctionDescriptor.builder()
                        .name("testfunction")
                        .params(ImmutableList.of(
                                string("field").build()
                        ))
                        .returnType(Boolean.class)
                        .build())
                .build();
        RuleBuilderStep step = mock(RuleBuilderStep.class);
        when(step.parameters()).thenReturn(Map.of("field", "my_field"));
        assertThatThrownBy(() -> ParserUtil.generateForFragment(step, incompleteFragment))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Could not replace all");
    }

    @Test
    public void generateForFragment_WhenAllParametersAreSet() {
        RuleBuilderStep step = mock(RuleBuilderStep.class);
        when(step.parameters()).thenReturn(Map.of("field", "my_field", "fieldValue", "my_value"));
        assertThat(ParserUtil.generateForFragment(step, ruleFragment)).isEqualTo(
                "( hasField(\"my_field\") and to_string($message.my_field)==\"my_value\" )"
        );
    }

    @Test
    public void addFragmentParameterThrowsException_WhenNoParametersPresent() {
        RuleBuilderStep step = mock(RuleBuilderStep.class);
        when(step.parameters()).thenReturn(null);
        ParameterDescriptor descriptor = string("test").build();
        assertThatThrownBy(() -> ParserUtil.addFragmentParameter(fragment, descriptor, step))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Cannot replace test");
    }

    @Test
    public void addFragmentParameterThrowsException_WhenParameterNotPresent() {
        RuleBuilderStep step = mock(RuleBuilderStep.class);
        when(step.parameters()).thenReturn(new HashMap<>());
        ParameterDescriptor descriptor = string("test").build();
        assertThatThrownBy(() -> ParserUtil.addFragmentParameter(fragment, descriptor, step))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Required parameter test");
    }

    @Test
    public void addFragmentParameterThrowsException_WhenParameterNotInFragment() {
        RuleBuilderStep step = mock(RuleBuilderStep.class);
        when(step.parameters()).thenReturn(Map.of("test", "val"));
        ParameterDescriptor descriptor = string("test").build();
        assertThatThrownBy(() -> ParserUtil.addFragmentParameter(fragment, descriptor, step))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Parameter test not set in fragment");
    }

    @Test
    public void addFragmentParameterReplacesParameter() {
        RuleBuilderStep step = mock(RuleBuilderStep.class);
        when(step.parameters()).thenReturn(Map.of("field", "my_field"));
        ParameterDescriptor descriptor = string("field").build();
        assertThat(ParserUtil.addFragmentParameter(fragment, descriptor, step)).isEqualTo(
                "( hasField(\"my_field\") and to_string($message.my_field)==\"{fieldValue}\" )"
        );
    }


}
