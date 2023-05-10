/*
 *  Copyright (C) 2020 Graylog, Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Server Side Public License, version 1,
 *  as published by MongoDB, Inc.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  Server Side Public License for more details.
 *
 *  You should have received a copy of the Server Side Public License
 *  along with this program. If not, see
 *  <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.pipelineprocessor.rulebuilder.parser;

import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderStep;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ParserUtilTest {

    private ParserUtil parserUtil = new ParserUtil();

    @Test
    public void addFunctionParameterNull_WhenNoParametersAreSet() {
        RuleBuilderStep step = mock(RuleBuilderStep.class);
        when(step.parameters()).thenReturn(null);
        ParameterDescriptor descriptor = mock(ParameterDescriptor.class);
        assertThat(parserUtil.addFunctionParameter(descriptor, step)).isNull();
    }

    @Test
    public void throwException_WhenNoParameterValueForRequiredParamIsSet() {
        RuleBuilderStep step = mock(RuleBuilderStep.class);
        Map<String, Object> params = new HashMap<>();
        when(step.parameters()).thenReturn(params);
        ParameterDescriptor descriptor = mock(ParameterDescriptor.class);
        when(descriptor.optional()).thenReturn(false);
        assertThatThrownBy(() -> parserUtil.addFunctionParameter(descriptor, step)).isInstanceOf(IllegalArgumentException.class);
    }


    @Test
    public void addFunctionParameterNull_WhenNoParameterValueIsSet() {
        RuleBuilderStep step = mock(RuleBuilderStep.class);
        Map<String, Object> params = new HashMap<>();
        when(step.parameters()).thenReturn(params);
        ParameterDescriptor descriptor = mock(ParameterDescriptor.class);
        when(descriptor.optional()).thenReturn(true);
        assertThat(parserUtil.addFunctionParameter(descriptor, step)).isNull();
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
        assertThat(parserUtil.addFunctionParameter(descriptor, step))
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
        assertThat(parserUtil.addFunctionParameter(descriptor, step))
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
        assertThat(parserUtil.addFunctionParameter(descriptor, step))
                .isEqualTo("    foo : bar");
    }

}
