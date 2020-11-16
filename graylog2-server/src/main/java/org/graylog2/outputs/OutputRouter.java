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

import org.graylog2.plugin.Message;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

public class OutputRouter {
    private final MessageOutput defaultMessageOutput;
    private final OutputRegistry outputRegistry;

    @Inject
    public OutputRouter(@DefaultMessageOutput MessageOutput defaultMessageOutput,
                        OutputRegistry outputRegistry) {
        this.defaultMessageOutput = defaultMessageOutput;
        this.outputRegistry = outputRegistry;
    }

    protected Set<MessageOutput> getMessageOutputsForStream(Stream stream) {
        Set<MessageOutput> result = new HashSet<>();
        for (Output output : stream.getOutputs()) {
            final MessageOutput messageOutput = outputRegistry.getOutputForIdAndStream(output.getId(), stream);
            if (messageOutput != null) {
                result.add(messageOutput);
            }
        }

        return result;
    }

    public Set<MessageOutput> getOutputsForMessage(final Message msg) {
        final Set<MessageOutput> result = getStreamOutputsForMessage(msg);
        result.add(defaultMessageOutput);

        return result;
    }

    public Set<MessageOutput> getStreamOutputsForMessage(final Message msg) {
        final Set<MessageOutput> result = new HashSet<>();

        for (Stream stream : msg.getStreams()) {
            result.addAll(getMessageOutputsForStream(stream));
        }

        return result;
    }
}
