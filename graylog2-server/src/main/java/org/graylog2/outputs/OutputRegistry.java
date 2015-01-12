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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.OutputService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Singleton
public class OutputRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(OutputRegistry.class);

    private final Cache<String, MessageOutput> runningMessageOutputs;
    private final MessageOutput defaultMessageOutput;
    private final OutputService outputService;
    private final MessageOutputFactory messageOutputFactory;

    @Inject
    public OutputRegistry(@DefaultMessageOutput MessageOutput defaultMessageOutput,
                          final OutputService outputService,
                          MessageOutputFactory messageOutputFactory) {
        this.defaultMessageOutput = defaultMessageOutput;
        this.outputService = outputService;
        this.messageOutputFactory = messageOutputFactory;
        this.runningMessageOutputs = CacheBuilder.newBuilder().build();
    }

    public MessageOutput getOutputForIdAndStream(String id, Stream stream) {
        try {
            return this.runningMessageOutputs.get(id, loadForIdAndStream(id, stream));
        } catch (ExecutionException e) {
            LOG.error("Unable to fetch output: ", e);
            return null;
        }
    }

    public Callable<MessageOutput> loadForIdAndStream(final String id, final Stream stream) {
        return new Callable<MessageOutput>() {
            @Override
            public MessageOutput call() throws Exception {
                final Output output = outputService.load(id);
                return launchOutput(output, stream);
            }
        };
    }

    protected MessageOutput launchOutput(Output output, Stream stream) throws MessageOutputConfigurationException {
        final MessageOutput messageOutput = messageOutputFactory.fromStreamOutput(output, stream, new Configuration(output.getConfiguration()));
        if (messageOutput == null)
            throw new IllegalArgumentException("Failed to instantiate MessageOutput from Output: " + output);

        return messageOutput;
    }

    protected Map<String, MessageOutput> getRunningMessageOutputs() {
        return ImmutableMap.copyOf(runningMessageOutputs.asMap());
    }

    public Set<MessageOutput> getMessageOutputs() {
        final ImmutableSet.Builder<MessageOutput> builder = ImmutableSet.builder();

        builder.addAll(this.runningMessageOutputs.asMap().values());
        builder.add(defaultMessageOutput);
        return ImmutableSet.copyOf(builder.build());
    }

    public void removeOutput(Output output) {
        runningMessageOutputs.invalidate(output.getId());
    }
}
