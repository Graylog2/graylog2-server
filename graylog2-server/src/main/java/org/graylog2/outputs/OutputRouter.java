/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.graylog2.outputs;

import org.graylog2.plugin.Message;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamOutput;
import org.graylog2.streams.StreamOutputService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class OutputRouter {
    protected final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private final MessageOutput defaultMessageOutput;
    private final StreamOutputService streamOutputService;
    private final MessageOutputFactory messageOutputFactory;

    @Inject
    public OutputRouter(@DefaultMessageOutput MessageOutput defaultMessageOutput,
                        StreamOutputService streamOutputService,
                        MessageOutputFactory messageOutputFactory) {
        this.defaultMessageOutput = defaultMessageOutput;
        this.streamOutputService = streamOutputService;
        this.messageOutputFactory = messageOutputFactory;
    }

    protected Map<StreamOutput, MessageOutput> getAllConfiguredMessageOutputs() {
        Map<StreamOutput, MessageOutput> result = new HashMap<>();

        for (StreamOutput streamOutput : streamOutputService.loadAll()) {
            final MessageOutput messageOutput;
            try {
                messageOutput = messageOutputFactory.fromStreamOutput(streamOutput);
                result.put(streamOutput, messageOutput);
            } catch (Exception e) {
                LOG.error("Unable to instantiate MessageOutput: " + e);
            }
        }

        return result;
    }

    public Set<MessageOutput> getOutputsForMessage(Message msg) {
        Set<MessageOutput> result = new HashSet<>();

        result.add(defaultMessageOutput);

        Map<StreamOutput, MessageOutput> configuredMessageOutputs = getAllConfiguredMessageOutputs();
        for (Stream stream : msg.getStreams())
            for (StreamOutput streamOutput : streamOutputService.loadAllForStream(stream))
                result.add(configuredMessageOutputs.get(streamOutput));

        return result;
    }
}
