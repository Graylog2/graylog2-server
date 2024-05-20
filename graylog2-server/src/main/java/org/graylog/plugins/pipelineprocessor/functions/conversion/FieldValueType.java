package org.graylog.plugins.pipelineprocessor.functions.conversion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.google.common.collect.ImmutableList;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderFunctionGroup;
import org.graylog2.plugin.Message;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.type;

public class FieldValueType extends AbstractFunction<String> {
    public static final String NAME = "field_value_type";
    public static final String FIELD = "field";

    public static final String BOOLEAN = "boolean";
    public static final String STRING = "string";
    public static final String DOUBLE = "double";
    public static final String LONG = "long";
    public static final String LIST = "list";
    public static final String MAP = "map";
    public static final String VALUE_NODE = "value_node";
    public static final String ARRAY_NODE = "array_node";
    public static final String OBJECT_NODE = "object_node";

    private final ParameterDescriptor<String, String> fieldParam;
    private final ParameterDescriptor<Message, Message> messageParam;

    public FieldValueType() {
        fieldParam = ParameterDescriptor.string(FIELD).description("The field to get the value type of").build();
        messageParam = type("message", Message.class).optional().description("The message to use, defaults to '$message'").ruleBuilderVariable().build();
    }

    @Override
    public String evaluate(FunctionArgs args, EvaluationContext context) {
        final String field = fieldParam.required(args, context);
        final Message message = messageParam.optional(args, context).orElse(context.currentMessage());
        final var value = message.getField(field);

        if (value instanceof Boolean) {
            return BOOLEAN;
        }
        if (value instanceof String) {
            return STRING;
        }
        if (value instanceof Double) {
            return DOUBLE;
        }
        if (value instanceof Long) {
            return LONG;
        }
        if (value instanceof List) {
            return LIST;
        }
        if (value instanceof Map) {
            return MAP;
        }
        if (value instanceof ValueNode) {
            return VALUE_NODE;
        }
        if (value instanceof ArrayNode) {
            return ARRAY_NODE;
        }
        if (value instanceof ObjectNode) {
            return OBJECT_NODE;
        }

        return value.getClass().getSimpleName().toLowerCase(Locale.ROOT);
    }

    @Override
    public FunctionDescriptor<String> descriptor() {
        return FunctionDescriptor.<String>builder()
                .name(NAME)
                .returnType(String.class)
                .params(ImmutableList.of(fieldParam, messageParam))
                .description("Returns the type of value stored in a field")
                .ruleBuilderEnabled()
                .ruleBuilderName("Get the type of value stored in a field")
                .ruleBuilderTitle("Get the type of value stored in field '${field}'")
                .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.MESSAGE)
                .build();
    }
}
