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
package org.graylog2.inputs.kafka;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.inputs.codecs.RadioMessageCodec;
import org.graylog2.inputs.transports.KafkaTransport;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;

public class KafkaInput extends MessageInput {

    private static final String NAME = "Kafka Input";

    @AssistedInject
    public KafkaInput(@Assisted Configuration configuration,
                      MetricRegistry metricRegistry,
                      KafkaTransport.Factory transport,
                      RadioMessageCodec.Factory codec,
                      LocalMetricRegistry localRegistry,
                      Config config,
                      Descriptor descriptor) {
        this(metricRegistry,
             transport.create(configuration),
             codec.create(configuration),
             localRegistry,
             config,
             descriptor);
    }

    protected KafkaInput(MetricRegistry metricRegistry,
                         KafkaTransport radioKafkaTransport,
                         RadioMessageCodec radioMessageCodec,
                         LocalMetricRegistry localRegistry,
                         MessageInput.Config config,
                         MessageInput.Descriptor descriptor) {
        super(metricRegistry, radioKafkaTransport, localRegistry, radioMessageCodec, config, descriptor);
    }

    public interface Factory extends MessageInput.Factory<KafkaInput> {
        @Override
        KafkaInput create(Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Descriptor extends MessageInput.Descriptor {
        @Inject
        public Descriptor() {
            super(NAME, false, "http://www.graylog2.org/resources/documentation/sending/kafka");
        }
    }

    public static class Config extends MessageInput.Config {
        @Inject
        public Config(KafkaTransport.Factory transport, RadioMessageCodec.Factory codec) {
            super(transport.getConfig(), codec.getConfig());
        }
    }
}
