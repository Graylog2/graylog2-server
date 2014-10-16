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
package org.graylog2.shared.inputs;

import com.codahale.metrics.MetricSet;
import com.google.common.collect.Maps;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.bindings.InstantiationService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Map;

public class MessageInputFactory {
    private final InstantiationService instantiationService;
    private final Map<String, MessageInput.Factory<? extends MessageInput>> inputFactories;

    @Inject
    public MessageInputFactory(InstantiationService instantiationService,
                               Map<String, MessageInput.Factory<? extends MessageInput>> inputFactories) {
        this.instantiationService = instantiationService;
        this.inputFactories = inputFactories;
    }

    public MessageInput create(String type, Configuration configuration) throws NoSuchInputTypeException {
        if (inputFactories.containsKey(type)) {
            final MessageInput.Factory<? extends MessageInput> factory = inputFactories.get(type);
            final MessageInput messageInput = factory.create(configuration);
            return messageInput;
        }
        throw new NoSuchInputTypeException("There is no input of type <" + type + "> registered.");
    }

    public Map<String, String> getAvailableInputs() {
        Map<String, String> result = Maps.newHashMap();
        // TODO what a crap, the modules should register descriptors instead
        for (Map.Entry<String, MessageInput.Factory<? extends MessageInput>> s : inputFactories.entrySet()) {
            final MessageInput input2 = s.getValue().create(
                    new Configuration(Maps.<String, Object>newHashMap()),
                    new Transport() {
                        @Override
                        public void setMessageAggregator(CodecAggregator aggregator) {

                        }

                        @Override
                        public void launch(MessageInput input) throws MisfireException {

                        }

                        @Override
                        public void stop() {

                        }

                        @Override
                        public ConfigurationRequest getRequestedConfiguration() {
                            return new ConfigurationRequest();
                        }

                        @Override
                        public MetricSet getMetricSet() {
                            return null;
                        }
                    },
                    new Codec() {
                        @Nullable
                        @Override
                        public Message decode(@Nonnull RawMessage buffer) {
                            return null;
                        }

                        @Nullable
                        @Override
                        public CodecAggregator getAggregator() {
                            return null;
                        }

                        @Override
                        public String getName() {
                            return null;
                        }

                        @Nonnull
                        @Override
                        public ConfigurationRequest getRequestedConfiguration() {
                            return new ConfigurationRequest();
                        }

                        @Override
                        public void overrideDefaultValues(@Nonnull ConfigurationRequest cr) {}

                    });
            result.put(s.getKey(), input2.getName());
        }

        return result;
    }
}