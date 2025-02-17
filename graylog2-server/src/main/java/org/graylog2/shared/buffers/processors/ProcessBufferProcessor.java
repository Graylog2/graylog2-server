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
package org.graylog2.shared.buffers.processors;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import jakarta.inject.Provider;
import org.graylog.failure.FailureSubmissionService;
import org.graylog2.buffers.OutputBuffer;
import org.graylog2.cluster.ClusterConfigChangedEvent;
import org.graylog2.messageprocessors.OrderedMessageProcessors;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Messages;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.buffers.MessageEvent;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.messageprocessors.MessageProcessor;
import org.graylog2.plugin.streams.DefaultStream;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.buffers.WorkHandler;
import org.graylog2.streams.StreamMetrics;
import org.graylog2.system.processing.ProcessingStatusRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.Strings.isNullOrEmpty;

public class ProcessBufferProcessor implements WorkHandler<MessageEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessBufferProcessor.class);

    private final Meter incomingMessages;
    private final Timer processTime;
    private final StreamMetrics streamMetrics;
    private final Meter outgoingMessages;
    private final OrderedMessageProcessors orderedMessageProcessors;

    private final OutputBuffer outputBuffer;
    private final ProcessingStatusRecorder processingStatusRecorder;
    private final MessageULIDGenerator messageULIDGenerator;
    private final DecodingProcessor decodingProcessor;
    private final Provider<Stream> defaultStreamProvider;
    private final FailureSubmissionService failureSubmissionService;
    private final ClusterConfigService clusterConfigService;
    private final EventBus eventBus;

    private volatile Message currentMessage;
    private volatile Duration cachedGracePeriod = null;

    @AssistedInject
    public ProcessBufferProcessor(MetricRegistry metricRegistry,
                                  OrderedMessageProcessors orderedMessageProcessors,
                                  OutputBuffer outputBuffer,
                                  ProcessingStatusRecorder processingStatusRecorder,
                                  MessageULIDGenerator messageULIDGenerator,
                                  @Assisted DecodingProcessor decodingProcessor,
                                  @DefaultStream Provider<Stream> defaultStreamProvider,
                                  FailureSubmissionService failureSubmissionService,
                                  StreamMetrics streamMetrics,
                                  ClusterConfigService clusterConfigService,
                                  EventBus eventBus) {
        this.orderedMessageProcessors = orderedMessageProcessors;
        this.outputBuffer = outputBuffer;
        this.processingStatusRecorder = processingStatusRecorder;
        this.messageULIDGenerator = messageULIDGenerator;
        this.decodingProcessor = decodingProcessor;
        this.defaultStreamProvider = defaultStreamProvider;
        this.failureSubmissionService = failureSubmissionService;
        this.clusterConfigService = clusterConfigService;
        this.eventBus = eventBus;

        incomingMessages = metricRegistry.meter(name(ProcessBufferProcessor.class, "incomingMessages"));
        outgoingMessages = metricRegistry.meter(name(ProcessBufferProcessor.class, "outgoingMessages"));
        processTime = metricRegistry.timer(name(ProcessBufferProcessor.class, "processTime"));
        this.streamMetrics = streamMetrics;
        currentMessage = null;

        eventBus.register(this);
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

    public Optional<Message> getCurrentMessage() {
        return Optional.ofNullable(currentMessage);
    }

    private void dispatchMessage(final Message msg) {
        currentMessage = msg;
        incomingMessages.mark();

        if (LOG.isTraceEnabled()) {
            LOG.trace("Starting to process message <{}>.", msg.getId());
        }

        try (final Timer.Context ignored = processTime.time()) {
            handleMessage(msg);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Finished processing message <{}>. Writing to output buffer.", msg.getId());
            }
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                // Log warning including the stacktrace
                LOG.warn("Unable to process message <{}>:", msg.getId(), e);
                // Log full message content to aid debugging
                LOG.debug("Failed message <{}>: {}", msg.getId(), msg.toDumpString());
            } else {
                // Only logs a single line warning without stacktrace
                LOG.warn("Unable to process message <{}>: {}", msg.getId(), e);
            }

            failureSubmissionService.submitUnknownProcessingError(msg, String.format(Locale.ENGLISH,
                    "Unable to process message <%s>: %s",
                    msg.getId(), e));

        } finally {
            currentMessage = null;
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

            message.getStreams().forEach(s -> streamMetrics.markIncomingMeter(s.getId()));
            message.ensureValidTimestamp();
            message.normalizeTimestamp(getTimeStampGracePeriod());

            // If a message is received via the Cluster-to-Cluster Forwarder, it already has this field set
            if (!message.hasField(Message.FIELD_GL2_MESSAGE_ID) || isNullOrEmpty(message.getFieldAs(String.class, Message.FIELD_GL2_MESSAGE_ID))) {
                // Set the message ID once all message processors have finished
                // See documentation of Message.FIELD_GL2_MESSAGE_ID for details
                message.addField(Message.FIELD_GL2_MESSAGE_ID, messageULIDGenerator.createULID(message));
            }

            // The processing time should only be set once all message processors have finished
            message.setProcessingTime(Tools.nowUTC());
            processingStatusRecorder.updatePostProcessingReceiveTime(message.getReceiveTime());

            if (failureSubmissionService.submitProcessingErrors(message)) {
                outputBuffer.insertBlocking(message);
            }
        }
    }

    public Duration getTimeStampGracePeriod() {
        if (cachedGracePeriod == null) {
            cachedGracePeriod = clusterConfigService.getOrDefault(TimeStampConfig.class, TimeStampConfig.getDefault()).gracePeriod();
        }
        return cachedGracePeriod;
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void handleGracePeriodUpdated(ClusterConfigChangedEvent event) {
        if (TimeStampConfig.class.getName().equals(event.type())) {
            cachedGracePeriod = null;
        }
    }

    public interface Factory {
        ProcessBufferProcessor create(DecodingProcessor decodingProcessor);
    }

    @Override
    public void onShutdown() {
        eventBus.unregister(this);
    }
}
