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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.outputs.filter.OutputFilter;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.outputs.FilteredMessageOutput;
import org.graylog2.plugin.outputs.MessageOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

@Singleton
public class MessageFilterOutput implements MessageOutput {
    private static final Logger LOG = LoggerFactory.getLogger(MessageFilterOutput.class);

    private final Set<FilteredMessageOutput> outputs;
    private final OutputFilter outputFilter;

    @Inject
    public MessageFilterOutput(Set<FilteredMessageOutput> outputs, OutputFilter outputFilter) {
        this.outputs = outputs;
        this.outputFilter = outputFilter;

        if (outputs.isEmpty()) {
            throw new IllegalStateException("No registered outputs found!");
        }
    }

    @Override
    public boolean isRunning() {
        return true; // TODO: Is this okay for the default output?
    }

    @Override
    public void write(List<Message> messages) throws Exception {
        for (final var message : messages) {
            write(message);
        }
    }

    @Override
    public void write(Message message) throws Exception {
        final var filteredMessage = outputFilter.apply(message);

        for (final var output : outputs) {
            try {
                if (LOG.isDebugEnabled()) {
                    LOG.trace("Writing message <{}> to output <{}>", filteredMessage, output);
                }
                output.writeFiltered(filteredMessage);
            } catch (Exception e) {
                LOG.error("Couldn't write message to output <{}>", output.getClass(), e);
            }
        }
    }

    @Override
    public void stop() {
        // TODO: Is there anything to do?
    }
}
