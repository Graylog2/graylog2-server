package org.graylog.plugins.pipelineprocessor.functions.messages;

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog2.plugin.Message;

import java.util.Locale;
import java.util.Map;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.type;

public class NormalizeFields extends AbstractFunction<Void> {

    public static final String NAME = "normalize_fields";
    private final ParameterDescriptor<Message, Message> messageParam;

    public NormalizeFields() {
        messageParam = type("message", Message.class).optional().description("The message to use, defaults to '$message'").build();
    }

    @Override
    public Void evaluate(FunctionArgs args, EvaluationContext context) {
        final Message message = context.currentMessage();
        final Map<String, Object> fields = message.getFields();
        for (String key : fields.keySet()) {
            message.removeField(key);
            message.addField(key.toLowerCase(Locale.ROOT), fields.get(key));
        }
        return null;
    }

    @Override
    public FunctionDescriptor<Void> descriptor() {
        return FunctionDescriptor.<Void>builder()
                .name(NAME)
                .returnType(Void.class)
                .params(ImmutableList.of(messageParam))
                .description("Normalizes all field names by setting them to lowercase")
                .build();
    }
}
