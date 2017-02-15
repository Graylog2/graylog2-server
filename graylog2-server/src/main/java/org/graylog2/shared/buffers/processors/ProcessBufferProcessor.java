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
package org.graylog2.shared.buffers.processors;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lmax.disruptor.WorkHandler;
import org.graylog2.buffers.OutputBuffer;
import org.graylog2.messageprocessors.OrderedMessageProcessors;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Messages;
import org.graylog2.plugin.buffers.MessageEvent;
import org.graylog2.plugin.messageprocessors.MessageProcessor;
import org.graylog2.plugin.streams.DefaultStream;
import org.graylog2.plugin.streams.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collection;

import static com.codahale.metrics.MetricRegistry.name;

public class ProcessBufferProcessor implements WorkHandler<MessageEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessBufferProcessor.class);

    private final Meter incomingMessages;

    private final Timer processTime;
    private final Meter outgoingMessages;
    private final OrderedMessageProcessors orderedMessageProcessors;

    private final OutputBuffer outputBuffer;
    private final DecodingProcessor decodingProcessor;
    private final Provider<Stream> defaultStreamProvider;

    @AssistedInject
    public ProcessBufferProcessor(MetricRegistry metricRegistry, OrderedMessageProcessors orderedMessageProcessors, OutputBuffer outputBuffer,
                                  @Assisted DecodingProcessor decodingProcessor, @DefaultStream Provider<Stream> defaultStreamProvider) {
        this.orderedMessageProcessors = orderedMessageProcessors;
        this.outputBuffer = outputBuffer;
        this.decodingProcessor = decodingProcessor;
        this.defaultStreamProvider = defaultStreamProvider;

        incomingMessages = metricRegistry.meter(name(ProcessBufferProcessor.class, "incomingMessages"));
        outgoingMessages = metricRegistry.meter(name(ProcessBufferProcessor.class, "outgoingMessages"));
        processTime = metricRegistry.timer(name(ProcessBufferProcessor.class, "processTime"));
    }

    @Override
    public void onEvent(MessageEvent event) throws Exception {
        try {
            // Decode the RawMessage to a Message object. The DecodingProcessor used to be a separate handler in the
            // ProcessBuffer. Due to performance problems discovered during 1.0.0 testing, we decided to move this here.
            // TODO The DecodingProcessor does not need to be a EventHandler. We decided to do it like this to keep the change as small as possible for 1.0.0.
            decodingProcessor.onEvent(event, 0L, false);

            if (event.isSingleMessage()) {
                dispatchMessage(event.getMessage());
            } else {
                final Collection<Message> messageList = event.getMessages();
                if (messageList == null) {
                    // skip message events which could not be decoded properly
                    return;
                }

                for (final Message message : messageList) {
                    dispatchMessage(message);
                }
            }
        } finally {
            event.clearMessages();
        }
    }

    private void dispatchMessage(final Message msg) {
        incomingMessages.mark();

        LOG.debug("Starting to process message <{}>.", msg.getId());

        try (final Timer.Context ignored = processTime.time()) {
            handleMessage(msg);
            LOG.debug("Finished processing message <{}>. Writing to output buffer.", msg.getId());
        } catch (Exception e) {
            LOG.warn("Unable to process message <{}>: {}", msg.getId(), e);
        } finally {
            outgoingMessages.mark();
        }
    }

    private void handleMessage(@Nonnull Message msg) {
        msg.addStream(defaultStreamProvider.get());
        Messages messages = msg;

        for (MessageProcessor messageProcessor : orderedMessageProcessors) {
            messages = messageProcessor.process(messages);
        }
        for (Message message : messages) {
            outputBuffer.insertBlocking(message);
        }
    }

    public interface Factory {
        ProcessBufferProcessor create(DecodingProcessor decodingProcessor);
    }
}
