/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.buffers.processors;

import com.codahale.metrics.MetricRegistry;
import org.graylog2.buffers.OutputBuffer;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Messages;
import org.graylog2.plugin.messageprocessors.MessageProcessor;
import org.graylog2.shared.buffers.processors.ProcessBufferProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class ServerProcessBufferProcessor extends ProcessBufferProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ServerProcessBufferProcessor.class);

    private final Iterable<MessageProcessor> messageProcessors;
    private final OutputBuffer outputBuffer;
    private final boolean sanitizeFieldNames;
    private final String replacement;

    @Inject
    public ServerProcessBufferProcessor(MetricRegistry metricRegistry,
                                        Iterable<MessageProcessor> messageProcessors,
                                        OutputBuffer outputBuffer,
                                        @Named("elasticsearch_sanitize_field_names") boolean sanitizeFieldNames,
                                        @Named("elasticsearch_invalid_character_replacement") String replacement) {
        super(metricRegistry);
        this.messageProcessors = requireNonNull(messageProcessors);
        this.outputBuffer = requireNonNull(outputBuffer);
        this.sanitizeFieldNames = sanitizeFieldNames;
        this.replacement = requireNonNull(replacement);
    }

    @Override
    protected void handleMessage(@Nonnull Message msg) {
        Messages messages = msg;

        if (sanitizeFieldNames) {
            for (Message message : messages) {
                replaceInvalidFieldNames(message);
            }
        }
        for (MessageProcessor messageProcessor : messageProcessors) {
            messages = messageProcessor.process(messages);
        }
        for (Message message : messages) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Finished processing message <{}>. Writing to output buffer.", message.getId());
            }
            outputBuffer.insertBlocking(message);
        }
    }

    private void replaceInvalidFieldNames(Message message) {
        final Map<String, Object> fields = message.getFields();
        for (Map.Entry<String, Object> field : fields.entrySet()) {
            final String fieldName = field.getKey();
            if (fieldName.contains(".")) {
                final String sanitizedFieldName = fieldName.replace(".", replacement);
                final Object value = field.getValue();
                message.removeField(fieldName);
                final Object oldFieldValue = message.addField(sanitizedFieldName, value);

                if (LOG.isWarnEnabled() && oldFieldValue != null) {
                    LOG.warn("Overwrote existing field [{}] after renaming [{}] in message <{}>",
                            sanitizedFieldName, fieldName, message.getId());
                }

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Replaced field name [{}] with [{}] in message <{}>",
                            fieldName, sanitizedFieldName, message.getId());
                }
            }
        }
    }
}
