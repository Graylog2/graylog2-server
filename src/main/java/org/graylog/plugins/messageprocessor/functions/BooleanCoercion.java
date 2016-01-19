package org.graylog.plugins.messageprocessor.functions;

import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog.plugins.messageprocessor.ast.functions.Function;
import org.graylog.plugins.messageprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.messageprocessor.ast.functions.FunctionDescriptor;

import static com.google.common.collect.ImmutableList.of;
import static org.graylog.plugins.messageprocessor.ast.functions.ParameterDescriptor.object;

public class BooleanCoercion implements Function<Boolean> {
    public static final String NAME = "bool";

    private static final String VALUE = "value";

    @Override
    public Boolean evaluate(FunctionArgs args, EvaluationContext context) {
        final Object evaluated = args.evaluated(VALUE, context, Object.class).orElse("false");
        return Boolean.parseBoolean(evaluated.toString());
    }

    @Override
    public FunctionDescriptor<Boolean> descriptor() {
        return FunctionDescriptor.<Boolean>builder()
                .name(NAME)
                .returnType(Boolean.class)
                .params(of(object(VALUE)))
                .build();
    }
}
