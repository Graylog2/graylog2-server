package org.graylog.plugins.messageprocessor.ast.functions.builtin;

import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog.plugins.messageprocessor.ast.expressions.Expression;
import org.graylog.plugins.messageprocessor.ast.functions.Function;
import org.graylog.plugins.messageprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.messageprocessor.ast.functions.ParameterDescriptor;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.shared.inputs.InputRegistry;

import javax.inject.Inject;
import java.util.Map;

import static com.google.common.collect.ImmutableList.of;

public class InputFunction implements Function<MessageInput> {

    public static final String NAME = "input";

    private final InputRegistry inputRegistry;

    @Inject
    public InputFunction(InputRegistry inputRegistry) {
        this.inputRegistry = inputRegistry;
    }

    @Override
    public MessageInput evaluate(Map<String, Expression> args, EvaluationContext context, Message message) {
        final Object id = args.get("id").evaluate(context, message);
        final IOState<MessageInput> inputState = inputRegistry.getInputState(id.toString());
        return inputState.getStoppable();
    }

    @Override
    public FunctionDescriptor<MessageInput> descriptor() {
        return FunctionDescriptor.<MessageInput>builder()
                .name(NAME)
                .returnType(MessageInput.class)
                .params(of(ParameterDescriptor.string("id")))
                .build();
    }
}
