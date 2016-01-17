package org.graylog.plugins.messageprocessor.functions;

import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog.plugins.messageprocessor.ast.functions.Function;
import org.graylog.plugins.messageprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.messageprocessor.ast.functions.FunctionDescriptor;

import java.util.Optional;

import static com.google.common.collect.ImmutableList.of;
import static org.graylog.plugins.messageprocessor.ast.functions.ParameterDescriptor.object;
import static org.graylog.plugins.messageprocessor.ast.functions.ParameterDescriptor.string;

public class SetField implements Function<Void> {

    public static final String NAME = "set_field";
    public static final String FIELD = "field";
    public static final String VALUE = "value";

    @Override
    public Void evaluate(FunctionArgs args, EvaluationContext context) {
        final Optional<Object> field = args.evaluated(FIELD, context, Object.class);
        final Optional<Object> value = args.evaluated(VALUE, context, Object.class);

        context.currentMessage().addField(field.get().toString(), value.get());

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
