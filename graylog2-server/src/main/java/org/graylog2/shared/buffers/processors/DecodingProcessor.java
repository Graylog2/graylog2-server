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

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lmax.disruptor.EventHandler;
import org.graylog2.plugin.GlobalMetricNames;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.ResolvableInetSocketAddress;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.buffers.MessageEvent;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.codecs.MultiMessageCodec;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.journal.Journal;
import org.graylog2.shared.messageq.MessageQueueAcknowledger;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

public class DecodingProcessor implements EventHandler<MessageEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(DecodingProcessor.class);

    private final Timer decodeTime;
    private final Counter decodedTrafficCounter;

    public interface Factory {
        DecodingProcessor create(@Assisted("decodeTime") Timer decodeTime, @Assisted("parseTime") Timer parseTime);
    }

    private final Map<String, Codec.Factory<? extends Codec>> codecFactory;
    private final ServerStatus serverStatus;
    private final MetricRegistry metricRegistry;
    private final Journal journal;
    private final MessageQueueAcknowledger acknowledger;
    private final Timer parseTime;

    @AssistedInject
    public DecodingProcessor(Map<String, Codec.Factory<? extends Codec>> codecFactory,
                             final ServerStatus serverStatus,
                             final MetricRegistry metricRegistry,
                             final Journal journal,
                             MessageQueueAcknowledger acknowledger,
                             @Assisted("decodeTime") Timer decodeTime,
                             @Assisted("parseTime") Timer parseTime) {
        this.codecFactory = codecFactory;
        this.serverStatus = serverStatus;
        this.metricRegistry = metricRegistry;
        this.journal = journal;
        this.acknowledger = acknowledger;

        // these metrics are global to all processors, thus they are passed in directly to avoid relying on the class name
        this.parseTime = parseTime;
        this.decodeTime = decodeTime;
        decodedTrafficCounter = metricRegistry.counter(GlobalMetricNames.DECODED_TRAFFIC);
    }

    @Override
    public void onEvent(MessageEvent event, long sequence, boolean endOfBatch) throws Exception {
        final Timer.Context context = decodeTime.time();
        try {
            processMessage(event);
        } catch (Exception e) {
            final RawMessage rawMessage = event.getRaw();
            LOG.error("Error processing message " + rawMessage, ExceptionUtils.getRootCause(e));

            // Mark message as processed to avoid keeping it in the journal.
            acknowledger.acknowledge(rawMessage.getMessageQueueId());
            journal.markJournalOffsetCommitted(rawMessage.getJournalOffset());

            // always clear the event fields, even if they are null, to avoid later stages to process old messages.
            // basically this will make sure old messages are cleared out early.
            event.clearMessages();
        } finally {
            if (event.getMessage() != null) {
                event.getMessage().recordTiming(serverStatus, "decode", context.stop());
            } else if (event.getMessages() != null) {
                for (final Message message : event.getMessages()) {
                    message.recordTiming(serverStatus, "decode", context.stop());
                }
            }
            // aid garbage collection to collect the raw message early (to avoid promoting it to later generations).
            event.clearRaw();
        }
    }

    private void processMessage(final MessageEvent event) throws ExecutionException {
        final RawMessage raw = event.getRaw();

        // for backwards compatibility: the last source node should contain the input we use.
        // this means that extractors etc defined on the prior inputs are silently ignored.
        // TODO fix the above
        String inputIdOnCurrentNode;
        try {
            // .inputId checked during raw message decode!
            inputIdOnCurrentNode = Iterables.getLast(raw.getSourceNodes()).inputId;
        } catch (NoSuchElementException e) {
            inputIdOnCurrentNode = null;
        }

        final Codec.Factory<? extends Codec> factory = codecFactory.get(raw.getCodecName());
        if (factory == null) {
            LOG.warn("Couldn't find factory for codec <{}>, skipping message {} on input <{}>.",
                    raw.getCodecName(), raw, inputIdOnCurrentNode);
            return;
        }

        final Codec codec = factory.create(raw.getCodecConfig());
        final String baseMetricName = name(codec.getClass(), inputIdOnCurrentNode);

        Message message = null;
        Collection<Message> messages = null;

        final Timer.Context decodeTimeCtx = parseTime.time();
        final long decodeTime;
        try {
            // This is ugly but needed for backwards compatibility of the Codec interface in 1.x.
            // TODO The Codec interface should be changed for 2.0 to support collections of messages so we can remove this hack.
            if (codec instanceof MultiMessageCodec) {
                messages = ((MultiMessageCodec) codec).decodeMessages(raw);
            } else {
                message = codec.decode(raw);
            }
        } catch (RuntimeException e) {
            LOG.error("Unable to decode raw message {} on input <{}>.", raw, inputIdOnCurrentNode);
            metricRegistry.meter(name(baseMetricName, "failures")).mark();
            throw e;
        } finally {
            decodeTime = decodeTimeCtx.stop();
        }

        if (message != null) {
            event.setMessage(postProcessMessage(raw, codec, inputIdOnCurrentNode, baseMetricName, message, decodeTime));
        } else if (messages != null && !messages.isEmpty()) {
            final List<Message> processedMessages = Lists.newArrayListWithCapacity(messages.size());

            for (final Message msg : messages) {
                final Message processedMessage = postProcessMessage(raw, codec, inputIdOnCurrentNode, baseMetricName, msg, decodeTime);

                if (processedMessage != null) {
                    processedMessages.add(processedMessage);
                }
            }

            event.setMessages(processedMessages);
        }
    }

    @Nullable
    private Message postProcessMessage(RawMessage raw, Codec codec, String inputIdOnCurrentNode, String baseMetricName, Message message, long decodeTime) {
        if (message == null) {
            metricRegistry.meter(name(baseMetricName, "failures")).mark();
            return null;
        }
        if (!message.isComplete()) {
            metricRegistry.meter(name(baseMetricName, "incomplete")).mark();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Dropping incomplete message {} on input <{}>. Parsed fields: [{}]",
                        raw, inputIdOnCurrentNode, message.getFields());
            }
            return null;
        }

        message.setJournalOffset(raw.getJournalOffset());
        message.setMessageQueueId(raw.getMessageQueueId());
        message.recordTiming(serverStatus, "parse", decodeTime);
        metricRegistry.timer(name(baseMetricName, "parseTime")).update(decodeTime, TimeUnit.NANOSECONDS);

        for (final RawMessage.SourceNode node : raw.getSourceNodes()) {
            switch (node.type) {
                case SERVER:
                    // Always use the last source node.
                    if (message.getField(Message.FIELD_GL2_SOURCE_INPUT) != null) {
                        LOG.debug("Multiple server nodes ({} {}) set for message id {}",
                                message.getField(Message.FIELD_GL2_SOURCE_INPUT), node.nodeId, message.getId());
                    }
                    message.addField(Message.FIELD_GL2_SOURCE_INPUT, node.inputId);
                    message.addField(Message.FIELD_GL2_SOURCE_NODE, node.nodeId);
                    break;
                // TODO Due to be removed in Graylog 3.x
                case RADIO:
                    // Always use the last source node.
                    if (message.getField(Message.FIELD_GL2_SOURCE_RADIO_INPUT) != null) {
                        LOG.debug("Multiple radio nodes ({} {}) set for message id {}",
                                message.getField(Message.FIELD_GL2_SOURCE_RADIO_INPUT), node.nodeId, message.getId());
                    }
                    message.addField(Message.FIELD_GL2_SOURCE_RADIO_INPUT, node.inputId);
                    message.addField(Message.FIELD_GL2_SOURCE_RADIO, node.nodeId);
                    break;
            }
        }

        if (inputIdOnCurrentNode != null) {
            try {
                message.setSourceInputId(inputIdOnCurrentNode);
            } catch (RuntimeException e) {
                LOG.warn("Unable to find input with id " + inputIdOnCurrentNode + ", not setting input id in this message.", e);
            }
        }

        final ResolvableInetSocketAddress remoteAddress = raw.getRemoteAddress();
        if (remoteAddress != null) {
            final String addrString = InetAddresses.toAddrString(remoteAddress.getAddress());
            message.addField(Message.FIELD_GL2_REMOTE_IP, addrString);
            if (remoteAddress.getPort() > 0) {
                message.addField(Message.FIELD_GL2_REMOTE_PORT, remoteAddress.getPort());
            }
            if (remoteAddress.isReverseLookedUp()) { // avoid reverse lookup if the hostname is available
                message.addField(Message.FIELD_GL2_REMOTE_HOSTNAME, remoteAddress.getHostName());
            }
            if (Strings.isNullOrEmpty(message.getSource())) {
                message.setSource(addrString);
            }
        }

        if (codec.getConfiguration() != null && codec.getConfiguration().stringIsSet(Codec.Config.CK_OVERRIDE_SOURCE)) {
            message.setSource(codec.getConfiguration().getString(Codec.Config.CK_OVERRIDE_SOURCE));
        }

        // Make sure that there is a value for the source field.
        if (Strings.isNullOrEmpty(message.getSource())) {
            message.setSource("unknown");
        }

        // The raw message timestamp is the receive time of the message. It has been created before writing the raw
        // message to the journal.
        message.setReceiveTime(raw.getTimestamp());

        metricRegistry.meter(name(baseMetricName, "processedMessages")).mark();
        decodedTrafficCounter.inc(message.getSize());
        return message;
    }
}
