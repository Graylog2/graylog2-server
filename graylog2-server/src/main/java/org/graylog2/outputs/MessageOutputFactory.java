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

import com.google.common.base.Preconditions;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rest.resources.streams.outputs.AvailableOutputSummary;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class MessageOutputFactory {
    private final Map<String, MessageOutput.Factory<? extends MessageOutput>> availableOutputs;

    @Inject
    public MessageOutputFactory(Map<String, MessageOutput.Factory<? extends MessageOutput>> availableOutputs) {
        this.availableOutputs = availableOutputs;
    }

    public MessageOutput fromStreamOutput(Output output, final Stream stream, Configuration configuration) throws MessageOutputConfigurationException {
        Preconditions.checkNotNull(output);
        Preconditions.checkNotNull(stream);
        Preconditions.checkNotNull(configuration);

        final String outputType = output.getType();
        Preconditions.checkArgument(outputType != null);

        final MessageOutput.Factory<? extends MessageOutput> factory = this.availableOutputs.get(outputType);

        Preconditions.checkArgument(factory != null, "Output type is not supported: %s!", outputType);

        return factory.create(stream, configuration);
    }


    public Map<String, AvailableOutputSummary> getAvailableOutputs() {
        final Map<String, AvailableOutputSummary> result = new HashMap<>(availableOutputs.size());
        for (Map.Entry<String, MessageOutput.Factory<? extends MessageOutput>> messageOutputEntry : this.availableOutputs.entrySet()) {
            final MessageOutput.Factory messageOutputFactoryClass = messageOutputEntry.getValue();
            final MessageOutput.Descriptor descriptor = messageOutputFactoryClass.getDescriptor();

            final AvailableOutputSummary availableOutputSummary = AvailableOutputSummary.create(
                    descriptor.getName(),
                    messageOutputEntry.getKey(),
                    descriptor.getHumanName(),
                    descriptor.getLinkToDocs(),
                    messageOutputFactoryClass.getConfig().getRequestedConfiguration()
            );

            result.put(messageOutputEntry.getKey(), availableOutputSummary);
        }

        return result;
    }

    public MessageOutput.Factory<? extends MessageOutput> get(String type) {
        return this.availableOutputs.get(type);
    }
}
