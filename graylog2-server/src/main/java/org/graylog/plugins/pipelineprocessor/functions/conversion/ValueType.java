package org.graylog.plugins.pipelineprocessor.functions.conversion;

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderFunctionGroup;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.object;

public class ValueType extends AbstractFunction<String> {
    public static final String NAME = "value_type";

    private final ParameterDescriptor<Object, Object> valueParam;

    public ValueType() {
        valueParam = object("value").ruleBuilderVariable().description("Value to check").build();
    }

    @Override
    public String evaluate(FunctionArgs args, EvaluationContext context) {
        final Object value = valueParam.required(args, context);
        if (value == null) {
            return null;
        }
        System.out.println(value);
        return value.getClass().getSimpleName();
    }

    @Override
    public FunctionDescriptor<String> descriptor() {
        return FunctionDescriptor.<String>builder()
                .name(NAME)
                .returnType(String.class)
                .params(valueParam)
                .description("Returns the type of this value")
                .ruleBuilderEnabled(false)
                .ruleBuilderName("Return the type of this value")
                .ruleBuilderTitle("Return the type of '${value}'")
                .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.STRING)
                .build();
    }
}
