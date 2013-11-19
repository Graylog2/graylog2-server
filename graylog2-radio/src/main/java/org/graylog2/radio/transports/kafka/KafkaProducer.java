/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
 *
 */
package org.graylog2.radio.transports.kafka;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import org.graylog2.plugin.Message;
import org.graylog2.radio.Radio;
import org.graylog2.radio.transports.RadioTransport;

import java.util.Properties;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class KafkaProducer implements RadioTransport {

    public final static String KAFKA_TOPIC = "graylog2-radio-messages";

    private final Producer<String, String> producer;

    public KafkaProducer(Radio radio) {
        Properties props = new Properties();
        props.put("metadata.broker.list", radio.getConfiguration().getKafkaBrokers());
        props.put("serializer.class", "kafka.serializer.StringEncoder");
        props.put("request.required.acks", String.valueOf(radio.getConfiguration().getKafkaRequiredAcks()));

        ProducerConfig config = new ProducerConfig(props);
        producer = new Producer<String, String>(config);
    }

    @Override
    public void send(Message msg) {
        // TODO serialize message lol
        KeyedMessage<String, String> data = new KeyedMessage<String, String>(KAFKA_TOPIC, null, msg.getMessage());

        producer.send(data);
    }

}
