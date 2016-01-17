package org.graylog.plugins.messageprocessor.functions;

import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog.plugins.messageprocessor.ast.functions.Function;
import org.graylog.plugins.messageprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.messageprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.messageprocessor.ast.functions.ParameterDescriptor;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.shared.inputs.InputRegistry;

import javax.inject.Inject;

import static com.google.common.collect.ImmutableList.of;

public class InputFunction implements Function<MessageInput> {

    public static final String NAME = "input";

    private final InputRegistry inputRegistry;

    @Inject
    public InputFunction(InputRegistry inputRegistry) {
        this.inputRegistry = inputRegistry;
    }

    @Override
    public MessageInput evaluate(FunctionArgs args, EvaluationContext context) {
        final String id = args.evaluated("id", context, String.class).orElse("");
        final IOState<MessageInput> inputState = inputRegistry.getInputState(id);
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
