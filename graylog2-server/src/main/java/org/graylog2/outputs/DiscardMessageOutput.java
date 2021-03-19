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
package org.graylog2.outputs;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.messageq.MessageQueueAcknowledger;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.codahale.metrics.MetricRegistry.name;

public class DiscardMessageOutput implements MessageOutput {
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final MessageQueueAcknowledger messageQueueAcknowledger;
    private final Meter messagesDiscarded;

    @AssistedInject
    public DiscardMessageOutput(final MessageQueueAcknowledger messageQueueAcknowledger,
                                final MetricRegistry metricRegistry,
                                @Assisted Stream stream,
                                @Assisted Configuration configuration) {
        this(messageQueueAcknowledger, metricRegistry);
    }

    @Inject
    public DiscardMessageOutput(final MessageQueueAcknowledger messageQueueAcknowledger, final MetricRegistry metricRegistry) {
        this.messageQueueAcknowledger = messageQueueAcknowledger;
        this.messagesDiscarded = metricRegistry.meter(name(this.getClass(), "messagesDiscarded"));
        isRunning.set(true);
    }

    @Override
    public void stop() {
        isRunning.set(false);
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }

    @Override
    public void write(Message message) throws Exception {
        messageQueueAcknowledger.acknowledge(message);
        messagesDiscarded.mark();
    }

    @Override
    public void write(List<Message> messages) throws Exception {
        messageQueueAcknowledger.acknowledge(messages);
        messagesDiscarded.mark(messages.size());
    }

    public interface Factory extends MessageOutput.Factory<DiscardMessageOutput> {
    }

    public static class Config extends MessageOutput.Config {
    }

    public static class Descriptor extends MessageOutput.Descriptor {
        public Descriptor() {
            super("Discard Message output", false, "", "Output that discards messages");
        }
    }
}
