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
package org.graylog.plugins.pipelineprocessor.functions.messages;

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderFunctionGroup;
import org.graylog2.plugin.Message;

import java.util.Map;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.type;

public class RenameFields extends AbstractFunction<Void> {

    public static final String NAME = "rename_fields";

    private final ParameterDescriptor<Map, Map> fieldsParam;
    private final ParameterDescriptor<Message, Message> messageParam;

    public RenameFields() {
        fieldsParam = type("fields", Map.class).ruleBuilderVariable().description("The map of old name keys to new name values.").build();
        messageParam = type("message", Message.class).optional().description("The message to use, defaults to '$message'").build();
    }

    @Override
    public Void evaluate(FunctionArgs args, EvaluationContext context) {
        final Map<String, String> fields = fieldsParam.required(args, context);
        final Message message = messageParam.optional(args, context).orElse(context.currentMessage());

        if (fields != null) {
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                final String oldName = entry.getKey();
                final String newName = entry.getValue();
                
                if (!oldName.equals(newName) && message.hasField(oldName)) {
                    message.addField(newName, message.getField(oldName));
                    message.removeField(oldName);
                }
            }
        }

        return null;
    }

    @Override
    public FunctionDescriptor<Void> descriptor() {
        return FunctionDescriptor.<Void>builder()
                .name(NAME)
                .returnType(Void.class)
                .params(fieldsParam, messageParam)
                .description("Rename message fields. If no specific message is provided, it performs the renaming operation on the currently processed message.")
                .ruleBuilderEnabled()
                .ruleBuilderName("Rename fields")
                .ruleBuilderTitle("Rename fields from map '${fields}'")
                .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.MESSAGE)
                .build();
    }
}
