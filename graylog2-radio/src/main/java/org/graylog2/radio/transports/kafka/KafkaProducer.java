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
package org.graylog2.radio.transports.kafka;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.RadioMessage;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.radio.Configuration;
import org.graylog2.radio.transports.RadioTransport;
import org.msgpack.MessagePack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static com.codahale.metrics.MetricRegistry.name;

public class KafkaProducer implements RadioTransport {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaProducer.class);
    public static final String KAFKA_TOPIC = "graylog2-radio-messages";

    private final Producer<byte[], byte[]> producer;
    private final MessagePack pack;
    private final Meter incomingMessages;
    private final Meter rejectedMessages;
    private final Timer processTime;

    @Inject
    public KafkaProducer(ServerStatus serverStatus, Configuration configuration, MetricRegistry metricRegistry) {
        pack = new MessagePack();

        Properties props = new Properties();
        props.put("metadata.broker.list", configuration.getKafkaBrokers());
        props.put("partitioner.class", "kafka.producer.DefaultPartitioner");
        props.put("serializer.class", "kafka.serializer.DefaultEncoder");
        props.put("request.required.acks", String.valueOf(configuration.getKafkaRequiredAcks()));
        props.put("client.id", "graylog2-radio-" + serverStatus.getNodeId().toString());
        props.put("producer.type", configuration.getKafkaProducerType());
        props.put("queue.buffering.max.ms", String.valueOf(configuration.getKafkaBatchMaxWaitMs()));
        props.put("batch.num.messages", String.valueOf(configuration.getKafkaBatchSize()));

        ProducerConfig config = new ProducerConfig(props);
        producer = new Producer<>(config);

        incomingMessages = metricRegistry.meter(name(KafkaProducer.class, "incomingMessages"));
        rejectedMessages = metricRegistry.meter(name(KafkaProducer.class, "rejectedMessages"));
        processTime = metricRegistry.timer(name(KafkaProducer.class, "processTime"));
    }

    @Override
    public void send(Message msg) {
        KeyedMessage<byte[], byte[]> data;

        try(Timer.Context context = processTime.time()) {
            incomingMessages.mark();
            data = new KeyedMessage<>(
                    KAFKA_TOPIC, msg.getId().getBytes(StandardCharsets.UTF_8), RadioMessage.serialize(pack, msg));

            producer.send(data);
        } catch(IOException e) {
            LOG.error("Could not serialize message.", e);
            rejectedMessages.mark();
        }
    }
}
