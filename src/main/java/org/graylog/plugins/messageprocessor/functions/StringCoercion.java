package org.graylog.plugins.messageprocessor.functions;

import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog.plugins.messageprocessor.ast.functions.Function;
import org.graylog.plugins.messageprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.messageprocessor.ast.functions.FunctionDescriptor;

import static com.google.common.collect.ImmutableList.of;
import static org.graylog.plugins.messageprocessor.ast.functions.ParameterDescriptor.object;
import static org.graylog.plugins.messageprocessor.ast.functions.ParameterDescriptor.param;

public class StringCoercion implements Function<String> {

    public static final String NAME = "string";

    private static final String VALUE = "value";
    private static final String DEFAULT = "default";

    @Override
    public String evaluate(FunctionArgs args, EvaluationContext context) {
        final Object evaluated = args.evaluated(VALUE, context, Object.class).orElse(new Object());
        if (evaluated instanceof String) {
            return (String) evaluated;
        } else {
            return args.evaluated(DEFAULT, context, String.class).orElse("");
        }
    }

    @Override
    public FunctionDescriptor<String> descriptor() {
        return FunctionDescriptor.<String>builder()
                .name(NAME)
                .returnType(String.class)
                .params(of(
                        object(VALUE),
                        param().optional().string(DEFAULT).build()
                ))
                .build();
    }
}
