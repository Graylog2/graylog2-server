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

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog2.plugin.Message;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.type;

public class DropMessage extends AbstractFunction<Void> {

    public static final String NAME = "drop_message";
    public static final String MESSAGE_ARG = "message";
    private final ParameterDescriptor<Message, Message> messageParam;

    public DropMessage() {
        messageParam = type(MESSAGE_ARG, Message.class).optional().description("The message to drop, defaults to '$message'").build();
    }

    @Override
    public Void evaluate(FunctionArgs args, EvaluationContext context) {
        final Message message = messageParam.optional(args, context).orElse(context.currentMessage());
        message.setFilterOut(true);
        return null;
    }

    @Override
    public FunctionDescriptor<Void> descriptor() {
        return FunctionDescriptor.<Void>builder()
                .name(NAME)
                .pure(true)
                .returnType(Void.class)
                .params(ImmutableList.of(
                        messageParam
                ))
                .description("Discards a message from further processing")
                .build();
    }
}
