package org.graylog.plugins.messageprocessor.ast.functions.builtin;

import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog.plugins.messageprocessor.ast.expressions.Expression;
import org.graylog.plugins.messageprocessor.ast.functions.Function;
import org.graylog.plugins.messageprocessor.ast.functions.FunctionDescriptor;
import org.graylog2.plugin.Message;

import java.util.Map;

import static com.google.common.collect.ImmutableList.of;
import static org.graylog.plugins.messageprocessor.ast.functions.ParameterDescriptor.object;
import static org.graylog.plugins.messageprocessor.ast.functions.ParameterDescriptor.string;

public class StringCoercion implements Function<String> {

    public static final String NAME = "string";

    private static final String VALUE = "value";
    private static final String DEFAULT = "default";

    @Override
    public String evaluate(Map<String, Expression> args, EvaluationContext context, Message message) {
        final Expression value = args.get(VALUE);
        final Object evaluated = value.evaluate(context, message);
        if (evaluated instanceof String) {
            return (String) evaluated;
        } else {
            return (String) args.get(DEFAULT).evaluate(context, message);
        }
    }

    @Override
    public FunctionDescriptor<String> descriptor() {
        return FunctionDescriptor.<String>builder()
                .name(NAME)
                .returnType(String.class)
                .params(of(
                        object(VALUE),
                        string(DEFAULT)
                ))
                .build();
    }
}
