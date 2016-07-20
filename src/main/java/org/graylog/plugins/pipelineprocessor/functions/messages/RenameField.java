package org.graylog.plugins.pipelineprocessor.functions.messages;

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog2.plugin.Message;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.type;

public class RenameField extends AbstractFunction<Void> {

    public static final String NAME = "rename_field";

    private final ParameterDescriptor<String, String> oldFieldParam;
    private final ParameterDescriptor<String, String> newFieldParam;
    private final ParameterDescriptor<Message, Message> messageParam;

    public RenameField() {
        oldFieldParam = string("old_field").build();
        newFieldParam = string("new_field").build();
        messageParam = type("message", Message.class).optional().build();
    }

    @Override
    public Void evaluate(FunctionArgs args, EvaluationContext context) {
        final String oldName = oldFieldParam.required(args, context);
        final String newName = newFieldParam.required(args, context);

        // exit early if the field names are the same (so we don't drop the field)
        if (oldName != null && oldName.equals(newName)) {
            return null;
        }
        final Message message = messageParam.optional(args, context).orElse(context.currentMessage());

        if (message.hasField(oldName)) {
            message.addField(newName, message.getField(oldName));
            message.removeField(oldName);
        }

        return null;
    }

    @Override
    public FunctionDescriptor<Void> descriptor() {
        return FunctionDescriptor.<Void>builder()
                .name(NAME)
                .returnType(Void.class)
                .params(oldFieldParam, newFieldParam, messageParam)
                .build();
    }
}
