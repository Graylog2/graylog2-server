/**
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
 */
package org.graylog2.outputs;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Output;
import org.graylog2.streams.OutputService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Singleton
public class OutputRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(OutputRegistry.class);

    private final Map<String, MessageOutput> runningMessageOutputs;
    private final MessageOutput defaultMessageOutput;
    private final OutputService outputService;
    private final MessageOutputFactory messageOutputFactory;

    @Inject
    public OutputRegistry(@DefaultMessageOutput MessageOutput defaultMessageOutput,
                          OutputService outputService,
                          MessageOutputFactory messageOutputFactory) {
        this.defaultMessageOutput = defaultMessageOutput;
        this.outputService = outputService;
        this.messageOutputFactory = messageOutputFactory;
        this.runningMessageOutputs = new HashMap<>();
    }

    public MessageOutput getOutputForId(String id) {
        if (!getRunningMessageOutputs().containsKey(id))
            try {
                final Output output = outputService.load(id);
                register(id, launchOutput(output));
            } catch (NotFoundException | MessageOutputConfigurationException e) {
                LOG.error("Unable to launch output <{}>: {}", id, e);
                return null;
            }
        return getRunningMessageOutputs().get(id);
    }

    protected void register(String id, MessageOutput output) {
        this.runningMessageOutputs.put(id, output);
    }

    protected MessageOutput launchOutput(Output output) throws MessageOutputConfigurationException {
        final MessageOutput messageOutput = messageOutputFactory.fromStreamOutput(output);
        if (messageOutput == null)
            throw new IllegalArgumentException("Failed to instantiate MessageOutput from Output: " + output);

        messageOutput.initialize(new Configuration(output.getConfiguration()));

        return messageOutput;
    }

    protected Map<String, MessageOutput> getRunningMessageOutputs() {
        return ImmutableMap.copyOf(runningMessageOutputs);
    }

    public Set<MessageOutput> getMessageOutputs() {
        Set<MessageOutput> runningOutputs = new HashSet<>(this.runningMessageOutputs.values());
        runningOutputs.add(defaultMessageOutput);
        return ImmutableSet.copyOf(runningOutputs);
    }
}
