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
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.type;

public class CloneMessage extends AbstractFunction<Message> {
    private static final Logger LOG = LoggerFactory.getLogger(CloneMessage.class);

    public static final String NAME = "clone_message";

    private final ParameterDescriptor<Message, Message> messageParam;

    public CloneMessage() {
        messageParam = type("message", Message.class).optional().description("The message to use, defaults to '$message'").build();
    }

    @Override
    public Message evaluate(FunctionArgs args, EvaluationContext context) {
        final Message currentMessage = messageParam.optional(args, context).orElse(context.currentMessage());

        final Object tsField = currentMessage.getField(Message.FIELD_TIMESTAMP);
        final Message clonedMessage;
        if (tsField instanceof DateTime) {
            clonedMessage = new Message(currentMessage.getMessage(), currentMessage.getSource(), currentMessage.getTimestamp());
            clonedMessage.addFields(currentMessage.getFields());
        } else {
            LOG.warn("Invalid timestamp <{}> (type: {}) in message <{}>. Using current time instead.",
                    tsField, tsField.getClass().getCanonicalName(), currentMessage.getId());

            final DateTime now = DateTime.now(DateTimeZone.UTC);
            clonedMessage = new Message(currentMessage.getMessage(), currentMessage.getSource(), now);
            clonedMessage.addFields(currentMessage.getFields());

            // Message#addFields() overwrites the "timestamp" field.
            clonedMessage.addField("timestamp", now);
            clonedMessage.addField("gl2_original_timestamp", String.valueOf(tsField));
        }

        clonedMessage.addStreams(currentMessage.getStreams());

        // register in context so the processor can inject it later on
        context.addCreatedMessage(clonedMessage);
        return clonedMessage;
    }

    @Override
    public FunctionDescriptor<Message> descriptor() {
        return FunctionDescriptor.<Message>builder()
                .name(NAME)
                .params(ImmutableList.of(messageParam))
                .returnType(Message.class)
                .description("Clones a message")
                .build();
    }
}
