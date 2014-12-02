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

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.util.Providers;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rest.resources.streams.outputs.AvailableOutputSummary;
import org.graylog2.shared.bindings.InstantiationService;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class MessageOutputFactory {
    private final InstantiationService instantiationService;
    private final Map<String, MessageOutput.Factory<? extends MessageOutput>> availableOutputs;

    @Inject
    public MessageOutputFactory(InstantiationService instantiationService,
                                Map<String, MessageOutput.Factory<? extends MessageOutput>> availableOutputs) {
        this.instantiationService = instantiationService;
        this.availableOutputs = availableOutputs;
    }

    public MessageOutput fromStreamOutput(Output output, final Stream stream, Configuration configuration) throws MessageOutputConfigurationException{
        return this.availableOutputs.get(output.getType()).create(stream, configuration);
    }

    public Map<String, AvailableOutputSummary> getAvailableOutputs() {
        Map<String, AvailableOutputSummary> result = new HashMap<>();

        for (Map.Entry<String, MessageOutput.Factory<? extends MessageOutput>> messageOutputEntry : this.availableOutputs.entrySet()) {
            final MessageOutput.Factory messageOutputFactoryClass = messageOutputEntry.getValue();
            AvailableOutputSummary availableOutputSummary = new AvailableOutputSummary();
            availableOutputSummary.requestedConfiguration = messageOutputFactoryClass.getConfig().getRequestedConfiguration();
            final MessageOutput.Descriptor descriptor = messageOutputFactoryClass.getDescriptor();
            availableOutputSummary.name = descriptor.getName();
            availableOutputSummary.type = messageOutputEntry.getKey();
            availableOutputSummary.humanName = descriptor.getHumanName();
            availableOutputSummary.linkToDocs = descriptor.getLinkToDocs();
            result.put(messageOutputEntry.getKey(), availableOutputSummary);
        }

        return result;
    }

    public MessageOutput.Factory<? extends MessageOutput> get(String type) {
        return this.availableOutputs.get(type);
    }
}
