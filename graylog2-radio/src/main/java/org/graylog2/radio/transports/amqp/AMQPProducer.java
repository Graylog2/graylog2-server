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
package org.graylog2.radio.transports.amqp;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.radio.Configuration;
import org.graylog2.radio.transports.RadioTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class AMQPProducer implements RadioTransport {
    private final ServerStatus serverStatus;

    private class AMQPSenderPool {
        private final int count;
        private final AMQPSender[] senders;
        private final AtomicInteger pointer;

        private AMQPSenderPool(int count, Configuration configuration) {
            this.count = count;
            this.senders = new AMQPSender[count];
            for (int i = 0; i < count; i++) {
                this.senders[i] = new AMQPSender(configuration.getAmqpHostname(),
                        configuration.getAmqpPort(),
                        String.format(configuration.getAmqpVirtualHost(), i),
                        configuration.getAmqpUsername(),
                        configuration.getAmqpPassword(),
                        String.format(configuration.getAmqpQueueName(), i),
                        configuration.getAmqpQueueType(),
                        String.format(configuration.getAmqpExchangeName(), i),
                        String.format(configuration.getAmqpRoutingKey(), i)
                );
            }

            this.pointer = new AtomicInteger(0);
        }
        public void send(Message msg) throws IOException {
            final int currentIndex = pointer.getAndIncrement();
            senders[currentIndex % count].send(msg);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(AMQPProducer.class);

    private final AMQPSenderPool senderPool;
    private final Meter incomingMessages;
    private final Meter rejectedMessages;
    private final Timer processTime;

    @Inject
    public AMQPProducer(MetricRegistry metricRegistry, Configuration configuration, ServerStatus serverStatus) {
        this.serverStatus = serverStatus;
        senderPool = new AMQPSenderPool(configuration.getAmqpParallelQueues(), configuration);
        incomingMessages = metricRegistry.meter(name(AMQPProducer.class, "incomingMessages"));
        rejectedMessages = metricRegistry.meter(name(AMQPProducer.class, "rejectedMessages"));
        processTime = metricRegistry.timer(name(AMQPProducer.class, "processTime"));
    }

    @Override
    public void send(Message msg) throws IOException {
        try (Timer.Context context = processTime.time()) {
            incomingMessages.mark();
            senderPool.send(msg);
        } catch (IOException e) {
            LOG.error("Could not write to AMQP.", e);
            rejectedMessages.mark();
            throw e;
        }
    }

}
