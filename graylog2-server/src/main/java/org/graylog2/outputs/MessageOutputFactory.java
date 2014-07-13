package org.graylog2.outputs;

import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.streams.StreamOutput;
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

    public MessageOutput fromStreamOutput(StreamOutput streamOutput) {
        final Class<? extends MessageOutput> messageOutputClass = findMessageOutputClassForStreamOutput(streamOutput.getType());
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
            result.put(messageOutputClass.getCanonicalName(), availableOutputSummary);
        }

        return result;
    }
}
