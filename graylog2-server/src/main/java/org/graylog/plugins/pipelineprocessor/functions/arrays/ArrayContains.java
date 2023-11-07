package org.graylog.plugins.pipelineprocessor.functions.arrays;

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderFunctionGroup;

import java.util.Collections;
import java.util.List;

public class ArrayContains extends AbstractArrayFunction<Boolean> {
    public static final String NAME = "array_contains";

    private final ParameterDescriptor<Object, List> elementsParam;
    private final ParameterDescriptor<Object, Object> valueParam;
    private final ParameterDescriptor<Boolean, Boolean> caseSensitiveParam;

    public ArrayContains() {
        elementsParam = ParameterDescriptor.type("elements", Object.class, List.class)
                .transform(AbstractArrayFunction::toList)
                .description("The input array, may be null")
                .build();
        valueParam = ParameterDescriptor.object("value")
                .description("The input value").build();
        caseSensitiveParam = ParameterDescriptor.bool("case_sensitive")
                .optional()
                .description("Whether or not to ignore case when checking string arrays").build();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Boolean evaluate(FunctionArgs args, EvaluationContext context) {
        final List<Object> elements = elementsParam.optional(args, context).orElse(Collections.emptyList());
        final Object value = valueParam.required(args, context);
        final boolean caseSensitive = caseSensitiveParam.optional(args, context).orElse(false);

        if (elements.isEmpty()) {
            return false;
        }

        if (!caseSensitive && containsStringValue(elements)) {
            return elements.stream()
                    .anyMatch(e -> e.toString().equalsIgnoreCase(String.valueOf(value)));
        }

        return elements.contains(value);
    }

    private static boolean containsStringValue(List elements) {
        return elements.get(0) instanceof String;
    }

    @Override
    public FunctionDescriptor<Boolean> descriptor() {
        return FunctionDescriptor.<Boolean>builder()
                .name(NAME)
                .pure(true)
                .returnType(Boolean.class)
                .params(ImmutableList.of(elementsParam, valueParam, caseSensitiveParam))
                .description("Checks if the specified element is contained in the array.")
                .ruleBuilderEnabled()
                .ruleBuilderName("Check if array contains value")
                .ruleBuilderTitle("Check if '${value}' is contained in array '${elements}'")
                .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.ARRAY)
                .build();
    }
}
