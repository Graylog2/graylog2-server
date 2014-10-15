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
package org.graylog2.radio.bindings.providers;

import com.codahale.metrics.MetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.radio.Configuration;
import org.graylog2.radio.transports.RadioTransport;
import org.graylog2.radio.transports.amqp.AMQPProducer;
import org.graylog2.radio.transports.kafka.KafkaProducer;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class RadioTransportProvider implements Provider<RadioTransport> {
    private final Configuration configuration;
    private final MetricRegistry metricRegistry;
    private final ServerStatus serverStatus;

    @Inject
    public RadioTransportProvider(Configuration configuration, MetricRegistry metricRegistry, ServerStatus serverStatus) {
        this.configuration = configuration;
        this.metricRegistry = metricRegistry;
        this.serverStatus = serverStatus;
    }

    @Override
    public RadioTransport get() {
        switch (configuration.getTransportType()) {
            case AMQP:
                return new AMQPProducer(metricRegistry, configuration, serverStatus);
            case KAFKA:
                return new KafkaProducer(serverStatus, configuration, metricRegistry);
            default:
                throw new RuntimeException("Cannot map transport type to transport.");
        }

    }
}
