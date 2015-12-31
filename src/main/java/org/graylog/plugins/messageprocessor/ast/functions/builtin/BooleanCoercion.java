package org.graylog.plugins.messageprocessor.ast.functions.builtin;

import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog.plugins.messageprocessor.ast.expressions.Expression;
import org.graylog.plugins.messageprocessor.ast.functions.Function;
import org.graylog.plugins.messageprocessor.ast.functions.FunctionDescriptor;
import org.graylog2.plugin.Message;

import java.util.Map;

import static com.google.common.collect.ImmutableList.of;
import static org.graylog.plugins.messageprocessor.ast.functions.ParameterDescriptor.object;

public class BooleanCoercion implements Function<Boolean> {
    public static final String NAME = "bool";

    private static final String VALUE = "value";

    @Override
    public Boolean evaluate(Map<String, Expression> args, EvaluationContext context, Message message) {
        final Expression value = args.get(VALUE);
        final Object evaluated = value.evaluate(context, message);
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
