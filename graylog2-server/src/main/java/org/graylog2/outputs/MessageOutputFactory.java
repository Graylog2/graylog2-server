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

import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.streams.Output;
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
    private final Set<Class<? extends MessageOutput>> availableMessageOutputClasses;
    private final InstantiationService instantiationService;

    @Inject
    public MessageOutputFactory(Set<Class<? extends MessageOutput>> availableMessageOutputClasses,
                                InstantiationService instantiationService) {
        this.availableMessageOutputClasses = availableMessageOutputClasses;
        this.instantiationService = instantiationService;
    }

    public MessageOutput fromStreamOutput(Output output) {
        final Class<? extends MessageOutput> messageOutputClass = findMessageOutputClassForStreamOutput(output.getType());
        final MessageOutput messageOutput = instantiationService.getInstance(messageOutputClass);

        return messageOutput;
    }

    private Class<? extends MessageOutput> findMessageOutputClassForStreamOutput(String type) {
        for (Class<? extends MessageOutput> messageOutputClass : availableMessageOutputClasses)
            if (messageOutputClass.getName().equals(type))
                return messageOutputClass;

        throw new IllegalArgumentException("No class found for type " + type);
    }

    public Map<String, AvailableOutputSummary> getAvailableOutputs() {
        Map<String, AvailableOutputSummary> result = new HashMap<>();

        for (Class<? extends MessageOutput> messageOutputClass : availableMessageOutputClasses) {
            MessageOutput messageOutput = instantiationService.getInstance(messageOutputClass);
            AvailableOutputSummary availableOutputSummary = new AvailableOutputSummary();
            availableOutputSummary.requestedConfiguration = messageOutput.getRequestedConfiguration();
            availableOutputSummary.name = messageOutput.getName();
            availableOutputSummary.type = messageOutput.getClass().getCanonicalName();
            availableOutputSummary.humanName = messageOutput.getHumanName();
            availableOutputSummary.linkToDocs = messageOutput.getLinkToDocs();
            result.put(messageOutputClass.getCanonicalName(), availableOutputSummary);
        }

        return result;
    }
}
