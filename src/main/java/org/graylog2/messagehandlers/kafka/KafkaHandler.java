/**
 * Copyright 2011 Dario Rexin <dario.rexin@r3-tech.de>
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

package org.graylog2.messagehandlers.kafka;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.KafkaMessageStream;
import kafka.javaapi.consumer.ConsumerConnector;


final public class KafkaHandler {
    public static String TYPE_SYSLOG = "syslog";
    public static String TYPE_GELF = "gelf";
    private static final Logger LOG = Logger.getLogger(KafkaHandler.class);

    public final static void start(final String host, final int port, final int timeout, final String groupId, final List<String> topics) {
        LOG.info("Starting Kafka handlers...");

        Properties props = new Properties();
        props.put("zk.connect", host+ ":" + port);
        props.put("zk.connectiontimeout.ms", String.valueOf(timeout));
        props.put("groupid", groupId);

        ConsumerConfig consumerConfig = new ConsumerConfig(props);
        ConsumerConnector consumerConnector = Consumer.createJavaConsumerConnector(consumerConfig);
        for(String topic : topics) {
            startHandlers(topic, consumerConnector);
        }
    }

    private final static void startHandlers(final String topicStr, final ConsumerConnector consumerConnector) {
        String[] splitConfig = topicStr.split(":");
        String topic = splitConfig[0];
        int threads = splitConfig.length > 1 ? Integer.valueOf(splitConfig[1]) : 1;
        String kind = splitConfig.length == 3 ? splitConfig[2] : TYPE_SYSLOG;

        Map<String, Integer> streamOptions = new HashMap<String, Integer>();
        streamOptions.put(topic, threads);
        Map<String, List<KafkaMessageStream>> topicMessageStreams =
                consumerConnector.createMessageStreams(streamOptions);
        List<KafkaMessageStream> streams = topicMessageStreams.get(topic);

        ExecutorService executor = Executors.newFixedThreadPool(threads);

        for(final KafkaMessageStream stream: streams) {
            executor.submit(new KafkaHandlerRunnable(stream, kind));
        }
    }
}
