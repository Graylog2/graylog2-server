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
    // We have two output lists for backwards compatibility.
    // See comments in MessageOutput.Factory and MessageOutput.Factory2 for details
    private final Map<String, MessageOutput.Factory<? extends MessageOutput>> availableOutputs;
    private final Map<String, MessageOutput.Factory2<? extends MessageOutput>> availableOutputs2;

    @Inject
    public MessageOutputFactory(Map<String, MessageOutput.Factory<? extends MessageOutput>> availableOutputs,
                                Map<String, MessageOutput.Factory2<? extends MessageOutput>> availableOutputs2) {
        this.availableOutputs = availableOutputs;
        this.availableOutputs2 = availableOutputs2;
    }

    public MessageOutput fromStreamOutput(Output output, final Stream stream, Configuration configuration) throws MessageOutputConfigurationException {
        Preconditions.checkNotNull(output);
        Preconditions.checkNotNull(stream);
        Preconditions.checkNotNull(configuration);

        final String outputType = output.getType();
        Preconditions.checkArgument(outputType != null);

        // We have two output lists for backwards compatibility.
        // See comments in MessageOutput.Factory and MessageOutput.Factory2 for details
        final MessageOutput.Factory2<? extends MessageOutput> factory2 = this.availableOutputs2.get(outputType);
        final MessageOutput.Factory<? extends MessageOutput> factory = this.availableOutputs.get(outputType);

        // If the same outputType value exists in both output factory maps for some reason, we want to prefer the
        // one that is using the more recent interface.
        if (factory2 != null) {
            return factory2.create(output, stream, configuration);
        } else if (factory != null) {
            return factory.create(stream, configuration);
        } else {
            throw new IllegalArgumentException("Output type is not supported: " + outputType);
        }
    }


    public Map<String, AvailableOutputSummary> getAvailableOutputs() {
        // We have two output lists for backwards compatibility.
        // See comments in MessageOutput.Factory and MessageOutput.Factory2 for details
        final Map<String, AvailableOutputSummary> result = new HashMap<>(availableOutputs.size());
        for (Map.Entry<String, MessageOutput.Factory<? extends MessageOutput>> messageOutputEntry : this.availableOutputs.entrySet()) {
            final MessageOutput.Factory messageOutputFactoryClass = messageOutputEntry.getValue();
            final MessageOutput.Descriptor descriptor = messageOutputFactoryClass.getDescriptor();

            final AvailableOutputSummary availableOutputSummary = createOutputSummary(messageOutputEntry.getKey(),
                    descriptor, messageOutputFactoryClass.getConfig());

            result.put(messageOutputEntry.getKey(), availableOutputSummary);
        }
        for (Map.Entry<String, MessageOutput.Factory2<? extends MessageOutput>> messageOutputEntry : this.availableOutputs2.entrySet()) {
            final MessageOutput.Factory2 messageOutputFactoryClass = messageOutputEntry.getValue();
            final MessageOutput.Descriptor descriptor = messageOutputFactoryClass.getDescriptor();

            final AvailableOutputSummary availableOutputSummary = createOutputSummary(messageOutputEntry.getKey(),
                    descriptor, messageOutputFactoryClass.getConfig());

            result.put(messageOutputEntry.getKey(), availableOutputSummary);
        }

        return result;
    }

    private AvailableOutputSummary createOutputSummary(String outputType,
                                                       MessageOutput.Descriptor descriptor,
                                                       MessageOutput.Config config) {
        return AvailableOutputSummary.create(
                descriptor.getName(),
                outputType,
                descriptor.getHumanName(),
                descriptor.getLinkToDocs(),
                config.getRequestedConfiguration()
        );
    }
}
