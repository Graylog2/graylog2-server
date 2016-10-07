/**
 * This file is part of Graylog Pipeline Processor.
 *
 * Graylog Pipeline Processor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Pipeline Processor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Pipeline Processor.  If not, see <http://www.gnu.org/licenses/>.
 */
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
        oldFieldParam = string("old_field").description("The old name of the field").build();
        newFieldParam = string("new_field").description("The new name of the field").build();
        messageParam = type("message", Message.class).optional().description("The message to use, defaults to '$message'").build();
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
                .description("Rename a message field")
                .build();
    }
}
