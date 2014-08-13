/*
 * Copyright 2012-2014 TORCH GmbH
 *
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

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.RadioMessage;
import org.graylog2.radio.Configuration;
import org.graylog2.radio.transports.RadioTransport;
import org.graylog2.plugin.ServerStatus;
import org.msgpack.MessagePack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Properties;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class KafkaProducer implements RadioTransport {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaProducer.class);

    public final static String KAFKA_TOPIC = "graylog2-radio-messages";

    private final Producer<byte[], byte[]> producer;

    private final MessagePack pack;

    @Inject
    public KafkaProducer(ServerStatus serverStatus, Configuration configuration) {

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
        producer = new Producer<byte[], byte[]>(config);
    }

    @Override
    public void send(Message msg) {
        KeyedMessage<byte[], byte[]> data;

        try {
            data = new KeyedMessage<byte[], byte[]>(KAFKA_TOPIC, msg.getId().getBytes(), RadioMessage.serialize(pack, msg));
        } catch(IOException e) {
            LOG.error("Could not serialize message.");
            return;
        }

        producer.send(data);
    }

}
