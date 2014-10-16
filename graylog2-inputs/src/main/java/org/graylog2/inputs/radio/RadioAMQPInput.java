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
package org.graylog2.inputs.radio;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.inputs.amqp.AMQPInput;
import org.graylog2.inputs.codecs.RadioMessageCodec;
import org.graylog2.inputs.transports.RadioAmqpTransport;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.transports.Transport;

public class RadioAMQPInput extends AMQPInput {

    @AssistedInject
    public RadioAMQPInput(@Assisted Configuration configuration,
                          MetricRegistry metricRegistry,
                          @Assisted Transport transport,
                          @Assisted Codec codec,
                          LocalMetricRegistry localRegistry) {
        super(configuration, metricRegistry, transport, codec, localRegistry);
    }

    @AssistedInject
    public RadioAMQPInput(@Assisted Configuration configuration,
                          MetricRegistry metricRegistry,
                          RadioAmqpTransport.Factory transport,
                          RadioMessageCodec.Factory codec,
                          LocalMetricRegistry localRegistry) {
        super(configuration, metricRegistry, transport.create(configuration), codec.create(configuration), localRegistry);
    }

    @Override
    public String getName() {
        return "Graylog2 Radio Input (AMQP)";
    }

    public interface Factory extends MessageInput.Factory<RadioAMQPInput> {
        @Override
        RadioAMQPInput create(Configuration configuration);

        @Override
        RadioAMQPInput create(Configuration configuration, Transport transport, Codec codec);
    }
}
