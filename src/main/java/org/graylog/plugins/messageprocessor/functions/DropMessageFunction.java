package org.graylog.plugins.messageprocessor.functions;

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog.plugins.messageprocessor.ast.functions.Function;
import org.graylog.plugins.messageprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.messageprocessor.ast.functions.FunctionDescriptor;
import org.graylog2.plugin.Message;

import java.util.Optional;

import static org.graylog.plugins.messageprocessor.ast.functions.ParameterDescriptor.param;

public class DropMessageFunction implements Function<Void> {

    public static final String NAME = "drop_message";

    @Override
    public Void evaluate(FunctionArgs args, EvaluationContext context) {
        final Optional<Message> message;
        if (args.isPresent("message")) {
            message = args.evaluated("message", context, Message.class);
        } else {
            message = Optional.of(context.currentMessage());
        }
        message.get().setFilterOut(true);
        return null;
    }

    @Override
    public FunctionDescriptor<Void> descriptor() {
        return FunctionDescriptor.<Void>builder()
                .name(NAME)
                .pure(true)
                .returnType(Void.class)
                .params(ImmutableList.of(
                        param().type(Message.class).optional().name("message").build()
                ))
                .build();
    }
}
