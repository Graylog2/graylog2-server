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
package org.graylog2.messageprocessors;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Messages;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.plugin.messageprocessors.MessageProcessor;
import org.graylog2.shared.buffers.processors.ProcessBufferProcessor;
import org.graylog2.shared.messageq.MessageQueueAcknowledger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static com.codahale.metrics.MetricRegistry.name;

public class MessageFilterChainProcessor implements MessageProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(MessageFilterChainProcessor.class);

    public static class Descriptor implements MessageProcessor.Descriptor {
        @Override
        public String name() {
            return "Message Filter Chain";
        }

        @Override
        public String className() {
            return MessageFilterChainProcessor.class.getCanonicalName();
        }
    }

    private final List<MessageFilter> filterRegistry;
    private final MetricRegistry metricRegistry;
    private final MessageQueueAcknowledger messageQueueAcknowledger;
    private final ServerStatus serverStatus;
    private final Meter filteredOutMessages;

    @Inject
    public MessageFilterChainProcessor(MetricRegistry metricRegistry,
                                       Set<MessageFilter> filterRegistry,
                                       MessageQueueAcknowledger messageQueueAcknowledger,
                                       ServerStatus serverStatus) {
        this.metricRegistry = metricRegistry;
        this.messageQueueAcknowledger = messageQueueAcknowledger;
        this.serverStatus = serverStatus;
        // we need to keep this sorted properly, so that the filters run in the correct order
        this.filterRegistry = Ordering.from(new Comparator<MessageFilter>() {
            @Override
            public int compare(MessageFilter filter1, MessageFilter filter2) {
                return ComparisonChain.start()
                        .compare(filter1.getPriority(), filter2.getPriority())
                        .compare(filter1.getName(), filter2.getName())
                        .result();
            }
        }).immutableSortedCopy(filterRegistry);

        if (filterRegistry.size() == 0)
            throw new RuntimeException("Empty filter registry!");

        this.filteredOutMessages = metricRegistry.meter(name(ProcessBufferProcessor.class, "filteredOutMessages"));
    }

    @Override
    public Messages process(Messages messages) {

        for (final MessageFilter filter : filterRegistry) {
            for (Message msg : messages) {
                final String timerName = name(filter.getClass(), "executionTime");
                final Timer timer = metricRegistry.timer(timerName);
                final Timer.Context timerContext = timer.time();

                try {
                    LOG.debug("Applying filter [{}] on message <{}>.", filter.getName(), msg.getId());

                    if (filter.filter(msg)) {
                        LOG.debug("Filter [{}] marked message <{}> to be discarded. Dropping message.",
                                filter.getName(),
                                msg.getId());
                        msg.setFilterOut(true);
                        filteredOutMessages.mark();
                        messageQueueAcknowledger.acknowledge(msg.getMessageQueueId());
                    }
                } catch (Exception e) {
                    LOG.error("Could not apply filter [" + filter.getName() + "] on message <" + msg.getId() + ">: ",
                            e);
                } finally {
                    final long elapsedNanos = timerContext.stop();
                    msg.recordTiming(serverStatus, timerName, elapsedNanos);
                }
            }
        }
        return messages;
    }

    @VisibleForTesting
    protected List<MessageFilter> getFilterRegistry() {
        return filterRegistry;
    }
}
