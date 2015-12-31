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

public class SetField implements Function<Void> {

    public static final String NAME = "set_field";
    public static final String FIELD = "field";
    public static final String VALUE = "value";

    @Override
    public Void evaluate(Map<String, Expression> args, EvaluationContext context, Message message) {
        final Object field = args.get(FIELD).evaluate(context, message);
        final Object value = args.get(VALUE).evaluate(context, message);
        message.addField(field.toString(), value);
        return null;
    }

    @Override
    public FunctionDescriptor<Void> descriptor() {
        return FunctionDescriptor.<Void>builder()
                .name(NAME)
                .returnType(Void.class)
                .params(of(string(FIELD),
                           object(VALUE)))
                .build();
    }
}
