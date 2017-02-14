/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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
