package org.graylog.plugins.pipelineprocessor.rulebuilder.rest;

import com.google.common.collect.Maps;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.functions.conversion.StringConversion;
import org.graylog.plugins.pipelineprocessor.functions.messages.HasField;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderStep;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RuleBuilderServiceTest {

    private static FunctionRegistry functionRegistry;
    private RuleBuilderService ruleBuilderService;

    @BeforeClass
    public static void registerFunctions() {
        final HashMap<String, Function<?>> functions = Maps.newHashMap();
        functions.put(StringConversion.NAME, new StringConversion());
        functions.put(HasField.NAME, new HasField());
        functionRegistry = new FunctionRegistry(functions);
    }

    @Before
    public void initialize() {
        ruleBuilderService = new RuleBuilderService(functionRegistry);
    }

    @Test
    public void addFunctionParameterNull_WhenNoParametersAreSet() {
        RuleBuilderStep step = mock(RuleBuilderStep.class);
        when(step.parameters()).thenReturn(null);
        ParameterDescriptor descriptor = mock(ParameterDescriptor.class);
        assertNull(ruleBuilderService.addFunctionParameter(descriptor, step));
    }

    @Test
    public void addFunctionParameterNull_WhenNoParameterValueIsSet() {
        RuleBuilderStep step = mock(RuleBuilderStep.class);
        Map<String, Object> params = new HashMap<>();
        when(step.parameters()).thenReturn(params);
        ParameterDescriptor descriptor = mock(ParameterDescriptor.class);
        assertNull(ruleBuilderService.addFunctionParameter(descriptor, step));
    }


}
