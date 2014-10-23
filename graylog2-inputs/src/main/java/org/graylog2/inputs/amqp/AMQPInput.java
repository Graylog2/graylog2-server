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
package org.graylog2.inputs.amqp;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.inputs.codecs.RadioMessageCodec;
import org.graylog2.inputs.transports.AmqpTransport;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.transports.Transport;

public class AMQPInput extends MessageInput {

    private static final String NAME = "AMQP Input";

    // solely for assistedinject subclass
    protected AMQPInput(MetricRegistry metricRegistry,
                        Transport transport,
                        Codec codec,
                        LocalMetricRegistry localRegistry,
                        MessageInput.Config config,
                        MessageInput.Descriptor descriptor) {
        super(metricRegistry, transport, localRegistry, codec, config, descriptor);
    }

    @AssistedInject
    public AMQPInput(@Assisted Configuration configuration,
                     MetricRegistry metricRegistry,
                     AmqpTransport.Factory transport,
                     RadioMessageCodec.Factory codec, LocalMetricRegistry localRegistry, Config config, Descriptor descriptor) {
        super(metricRegistry, transport.create(configuration), localRegistry, codec.create(configuration), config,
              descriptor);
    }

    public interface Factory extends MessageInput.Factory<AMQPInput> {
        @Override
        AMQPInput create(Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Descriptor extends MessageInput.Descriptor {
        @Inject
        public Descriptor() {
            super(NAME, false, "");
        }
    }

    public static class Config extends MessageInput.Config {
        @Inject
        public Config(AmqpTransport.Factory transport, RadioMessageCodec.Factory codec) {
            super(transport.getConfig(), codec.getConfig());
        }
    }
}
