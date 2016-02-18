package org.graylog.plugins.pipelineprocessor.functions.strings;

import org.apache.commons.lang3.StringUtils;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import static com.google.common.collect.ImmutableList.of;

public class Contains extends AbstractFunction<Boolean> {

    public static final String NAME = "contains";

    @Override
    public Boolean evaluate(FunctionArgs args, EvaluationContext context) {
        final String value = args.param("value").evalRequired(args, context, String.class);
        final String search = args.param("search").evalRequired(args, context, String.class);
        final boolean ignoreCase = args.param("ignore_case").eval(args, context, Boolean.class).orElse(false);
        if (ignoreCase) {
            return StringUtils.containsIgnoreCase(value, search);
        } else {
            return StringUtils.contains(value, search);
        }
    }

    @Override
    public FunctionDescriptor<Boolean> descriptor() {
        return FunctionDescriptor.<Boolean>builder()
                .name(NAME)
                .returnType(Boolean.class)
                .params(of(
                        ParameterDescriptor.string("value").build(),
                        ParameterDescriptor.string("search").build(),
                        ParameterDescriptor.bool("ignore_case").optional().build()
                ))
                .build();
    }
}
