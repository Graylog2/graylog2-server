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
package org.graylog2.inputs.radio;

import com.codahale.metrics.MetricRegistry;
import javax.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.inputs.codecs.RadioMessageCodec;
import org.graylog2.inputs.kafka.KafkaInput;
import org.graylog2.inputs.transports.RadioKafkaTransport;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;

public class RadioKafkaInput extends KafkaInput {

    private static final String NAME = "Graylog Radio Input (Kafka)";

    @AssistedInject
    public RadioKafkaInput(@Assisted Configuration configuration,
                           MetricRegistry metricRegistry,
                           RadioKafkaTransport.Factory transport,
                           RadioMessageCodec.Factory codec,
                           LocalMetricRegistry localRegistry,
                           Config config,
                           Descriptor descriptor, ServerStatus serverStatus) {
        super(metricRegistry,
                configuration,
                transport.create(configuration),
                codec.create(configuration),
                localRegistry,
                config,
                descriptor, serverStatus);
    }

    public interface Factory extends MessageInput.Factory<RadioKafkaInput> {
        @Override
        RadioKafkaInput create(Configuration configuration);

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
        public Config(RadioKafkaTransport.Factory transport, RadioMessageCodec.Factory codec) {
            super(transport.getConfig(), codec.getConfig());
        }
    }
}
