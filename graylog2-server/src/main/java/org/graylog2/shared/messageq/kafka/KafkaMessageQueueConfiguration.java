package org.graylog2.shared.messageq.kafka;

import com.github.joschi.jadconfig.Parameter;
import org.graylog2.plugin.PluginConfigBean;

public class KafkaMessageQueueConfiguration implements PluginConfigBean {
    public static final String KAFKA_MESSAGE_QUEUE_BOOTSTRAP_SERVERS = "kafka_message_queue_bootstrap_servers";

    @Parameter(value = KAFKA_MESSAGE_QUEUE_BOOTSTRAP_SERVERS, required = true)
    private String kafkaMessageQueueBootstrapServers;
}
