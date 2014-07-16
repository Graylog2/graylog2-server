/*
 * Copyright 2013-2014 TORCH GmbH
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
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.outputs;

import com.google.common.collect.ImmutableSet;
import org.graylog2.database.ValidationException;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.OutputService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.BadRequestException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Singleton
public class OutputRegistry {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private final Map<Stream, Set<MessageOutput>> registeredMessageOutputs;
    private final MessageOutput defaultMessageOutput;
    private final OutputService outputService;

    @Inject
    public OutputRegistry(@DefaultMessageOutput MessageOutput defaultMessageOutput,
                          OutputService outputService) {
        this.defaultMessageOutput = defaultMessageOutput;
        this.outputService = outputService;
        this.registeredMessageOutputs = new HashMap<>();
    }

    public Output createOutput(Output request) throws ValidationException {
        final Output output = outputService.create(request);

        final String id;
        try {
            id = outputService.save(output);
        } catch (ValidationException e) {
            LOG.error("Validation error.", e);
            throw new BadRequestException(e);
        }

        return output;
    }

    public void registerForStream(Stream stream, MessageOutput messageOutput) {
        final Set<MessageOutput> messageOutputs;
        if (this.registeredMessageOutputs.get(stream) == null) {
            messageOutputs = new HashSet<>();
        } else {
            messageOutputs = this.registeredMessageOutputs.get(stream);
        }

        messageOutputs.add(messageOutput);
    }

    public Set<MessageOutput> getMessageOutputsForStream(Stream stream) {
        Set<MessageOutput> result = new HashSet<>();
        result.add(defaultMessageOutput);
        result.addAll(this.registeredMessageOutputs.get(stream));

        return ImmutableSet.copyOf(result);
    }

    public Set<MessageOutput> getMessageOutputs() {
        Set<MessageOutput> result = new HashSet<>();
        result.add(defaultMessageOutput);

        for (Set<MessageOutput> messageOutputs : this.registeredMessageOutputs.values()) {
            result.addAll(messageOutputs);
        }

        return ImmutableSet.copyOf(result);
    }

    public void removeMessageOutput(MessageOutput messageOutput) {
        if (messageOutput.equals(defaultMessageOutput))
            throw new IllegalArgumentException("Cannot remove default message output");

        for (Set<MessageOutput> messageOutputs : registeredMessageOutputs.values())
            messageOutputs.remove(messageOutput);
    }

    public void removeMessageOutputFromStream(MessageOutput messageOutput, Stream stream) {
        if (messageOutput.equals(defaultMessageOutput))
            throw new IllegalArgumentException("Cannot remove default message output");

        registeredMessageOutputs.get(stream).remove(messageOutput);
    }
}
