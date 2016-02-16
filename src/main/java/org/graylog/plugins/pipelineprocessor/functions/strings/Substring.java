package org.graylog.plugins.pipelineprocessor.functions.strings;

import com.google.common.primitives.Ints;
import org.apache.commons.lang3.StringUtils;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import static com.google.common.collect.ImmutableList.of;

public class Substring extends AbstractFunction<String> {

    public static final String NAME = "substring";

    @Override
    public String evaluate(FunctionArgs args, EvaluationContext context) {
        final String value = args.param("value").evalRequired(args, context, String.class);
        final int start = Ints.saturatedCast(args.param("start").evalRequired(args, context, Long.class));
        final int end = Ints.saturatedCast(args.param("end").eval(args, context, Long.class).orElse((long) value.length()));

        return StringUtils.substring(value, start, end);
    }

    @Override
    public FunctionDescriptor<String> descriptor() {
        return FunctionDescriptor.<String>builder()
                .name(NAME)
                .returnType(String.class)
                .params(of(
                        ParameterDescriptor.string("value").build(),
                        ParameterDescriptor.integer("start").build(),
                        ParameterDescriptor.integer("end").optional().build()
                ))
                .build();
    }
}
