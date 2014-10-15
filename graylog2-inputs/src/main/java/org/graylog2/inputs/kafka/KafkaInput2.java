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
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.inputs.codecs.RadioMessageCodec;
import org.graylog2.inputs.transports.KafkaTransport;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput2;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.transports.Transport;

public class KafkaInput2 extends MessageInput2 {

    @AssistedInject
    public KafkaInput2(@Assisted Configuration configuration,
                       MetricRegistry metricRegistry,
                       @Assisted Transport transport,
                       @Assisted Codec codec) {
        super(metricRegistry, transport, codec);
    }

    @AssistedInject
    public KafkaInput2(@Assisted Configuration configuration,
                       MetricRegistry metricRegistry,
                       KafkaTransport.Factory transport,
                       RadioMessageCodec.Factory codec) {
        super(metricRegistry, transport.create(configuration), codec.create(configuration));
    }

    @Override
    public boolean isExclusive() {
        return false;
    }

    @Override
    public String getName() {
        return "Kafka Input (transport)";
    }

    @Override
    public String linkToDocs() {
        return "http://www.graylog2.org/resources/documentation/sending/kafka";
    }

    public interface Factory extends MessageInput2.Factory<KafkaInput2> {
        @Override
        KafkaInput2 create(Configuration configuration);

        @Override
        KafkaInput2 create(Configuration configuration, Transport transport, Codec codec);
    }
}
