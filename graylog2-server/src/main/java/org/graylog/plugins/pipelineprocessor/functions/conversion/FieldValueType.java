/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.pipelineprocessor.functions.conversion;

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

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.type;

public class FieldValueType extends AbstractFunction<String> {
    public static final String NAME = "field_value_type";
    public static final String FIELD = "field";

    public enum Type {
        BOOLEAN(Boolean.class, "boolean"),
        STRING(String.class, "string"),
        DOUBLE(Double.class, "double"),
        LONG(Long.class, "long"),
        LIST(List.class, "list"),
        MAP(Map.class,"map"),
        VALUE_NODE(ValueNode.class, "value_node"),
        ARRAY_NODE(ArrayNode.class, "array_node"),
        OBJECT_NODE(ObjectNode.class, "object_node"),
        NULL(Type.class, "null"),;

        private final Class<?> classType;
        private final String name;

        Type(Class<?> classType, String name) {
            this.classType = classType;
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public static Type forValue(Object value){
            if (value == null) {
                return NULL;
            }

            for (Type type: values()) {
                if (type.classType.isInstance(value)) {
                    return type;
                }
            }

            throw new IllegalArgumentException("No Type value mapped for " + value.getClass().getTypeName());
        }
    }

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

        try {
            return Type.forValue(value).getName();
        } catch (IllegalArgumentException e) {
            return value.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        }
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
