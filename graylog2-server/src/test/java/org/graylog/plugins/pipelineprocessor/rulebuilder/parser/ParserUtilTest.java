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
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderStep;
import org.graylog2.bindings.providers.SecureFreemarkerConfigProvider;
import org.junit.Before;
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

    Configuration configuration;

    @Before
    public void initializeFreemarkerConfig() {
        SecureFreemarkerConfigProvider secureFreemarkerConfigProvider = new SecureFreemarkerConfigProvider();
        this.configuration = secureFreemarkerConfigProvider.get();
        configuration.setLogTemplateExceptions(false);
        StringTemplateLoader templateLoader = new StringTemplateLoader();
        templateLoader.putTemplate("test_fragment1", "let gl2_fragmentvar_v1 = $message.${field};");
        templateLoader.putTemplate("test_fragment2", "let gl2_fragmentvar_v1 = $message.${field!\"defaultField\"};");
        configuration.setTemplateLoader(templateLoader);
    }

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
    public void addFunctionParameterNull_WhenNoParameterValueIsSet() {
        RuleBuilderStep step = mock(RuleBuilderStep.class);
        Map<String, Object> params = new HashMap<>();
        when(step.parameters()).thenReturn(params);
        ParameterDescriptor descriptor = mock(ParameterDescriptor.class);
        when(descriptor.optional()).thenReturn(true);
        assertThat(ParserUtil.addFunctionParameter(descriptor, step)).isNull();
    }

    @Test
    public void addFunctionParameterNull_WhenEmptyStringParameterValueIsSet() {
        String parameterName = "foo";
        var parameterValue = "";
        RuleBuilderStep step = mock(RuleBuilderStep.class);
        Map<String, Object> params = Map.of(parameterName, parameterValue);
        when(step.parameters()).thenReturn(params);
        ParameterDescriptor descriptor = mock(ParameterDescriptor.class);
        when(descriptor.name()).thenReturn(parameterName);
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

    @Test
    public void addFunctionParameterSyntaxOk_WhenVariableParameterStringContainsBadChar() {
        String parameterName = "foo";
        String parameterValue = "bar\"123";
        RuleBuilderStep step = mock(RuleBuilderStep.class);
        Map<String, Object> params = Map.of(parameterName, parameterValue);
        when(step.parameters()).thenReturn(params);
        ParameterDescriptor descriptor = mock(ParameterDescriptor.class);
        when(descriptor.name()).thenReturn(parameterName);
        assertThat(ParserUtil.addFunctionParameter(descriptor, step))
                .isEqualTo("    foo : \"bar\\\"123\"");
    }

    // Fragment Building

    @Test
    public void generateForFragmentThrowsException_WhenTemplateNotFound() {
        RuleBuilderStep step = mock(RuleBuilderStep.class);
        when(step.function()).thenReturn("unknown");
        assertThatThrownBy(() -> ParserUtil.generateForFragment(step, configuration))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void generateForFragmentThrowsException_WhenParameterNotSet() {
        RuleBuilderStep step = mock(RuleBuilderStep.class);
        when(step.function()).thenReturn("test_fragment1");
        assertThatThrownBy(() -> ParserUtil.generateForFragment(step, configuration))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void generateForFragmentConvertsFreemarkerTemplate() {
        RuleBuilderStep step = mock(RuleBuilderStep.class);
        when(step.function()).thenReturn("test_fragment1");
        Map<String, Object> params = Map.of("field", "my_field");
        when(step.parameters()).thenReturn(params);
        assertThat(ParserUtil.generateForFragment(step, configuration))
                .isEqualTo("let gl2_fragmentvar_v1 = $message.\"my_field\";");
    }


}
