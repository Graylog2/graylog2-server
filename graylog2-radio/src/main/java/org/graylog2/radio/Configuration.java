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
package org.graylog2.radio;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.validators.InetPortValidator;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.Tools;

import java.net.URI;

public class Configuration extends BaseConfiguration {
    private static final int RADIO_DEFAULT_PORT = 12950;

    public enum TRANSPORT_TYPE {
        AMQP, KAFKA
    }

    @Parameter(value = "node_id_file")
    private String nodeIdFile = "/etc/graylog2-radio-node-id";

    @Parameter(value = "transport_type", required = true)
    private String transportType = "amqp";

    @Parameter(value = "rest_listen_uri", required = true)
    private URI restListenUri = URI.create("http://127.0.0.1:" + RADIO_DEFAULT_PORT + "/");

    @Parameter(value = "graylog2_server_uri", required = true)
    private URI graylog2ServerUri;

    @Parameter(value = "rest_transport_uri")
    private URI restTransportUri;

    @Parameter(value = "kafka_brokers")
    private String kafkaBrokers;

    @Parameter(value = "kafka_required_acks")
    private int kafkaRequiredAcks = 1;

    @Parameter(value = "kafka_producer_type")
    private String kafkaProducerType = "async";

    @Parameter(value = "kafka_batch_size", validator = PositiveIntegerValidator.class)
    private int kafkaBatchSize = 200;

    @Parameter(value = "kafka_batch_max_wait_ms", validator = PositiveIntegerValidator.class)
    private int kafkaBatchMaxWaitMs = 250;

    @Parameter(value = "amqp_broker_hostname")
    private String amqpHostname = "localhost";

    @Parameter(value = "amqp_broker_port", validator = InetPortValidator.class)
    private int amqpPort = 5672;

    @Parameter(value = "amqp_broker_username")
    private String amqpUsername;

    @Parameter(value = "amqp_broker_password")
    private String amqpPassword;

    @Parameter(value = "amqp_broker_vhost")
    private String amqpVhost = "/";

    @Parameter(value = "amqp_broker_queue_name")
    private String amqpQueueName = "graylog2-radio-messages";

    @Parameter(value = "amqp_broker_queue_type")
    private String amqpQueueType = "topic";

    @Parameter(value = "amqp_broker_exchange_name")
    private String amqpExchangeName = "graylog2";

    @Parameter(value = "amqp_broker_routing_key")
    private String amqpRoutingKey = "graylog2-radio-message";

    @Parameter(value = "amqp_broker_parallel_queues")
    private int amqpParallelQueues = 1;

    @Parameter(value = "ring_size", required = true, validator = PositiveIntegerValidator.class)
    private int ringSize = 1024;

    public String getNodeIdFile() {
        return nodeIdFile;
    }

    public TRANSPORT_TYPE getTransportType() {
        try {
            return TRANSPORT_TYPE.valueOf(transportType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid [transport_type] configured: " + transportType);
        }
    }

    public URI getRestListenUri() {
        return Tools.getUriWithPort(restListenUri, RADIO_DEFAULT_PORT);
    }

    public URI getGraylog2ServerUri() {
        return graylog2ServerUri == null ? null : Tools.getUriWithPort(graylog2ServerUri, GRAYLOG2_DEFAULT_PORT);
    }

    public String getKafkaBrokers() {
        return kafkaBrokers;
    }

    public int getKafkaRequiredAcks() {
        return kafkaRequiredAcks;
    }

    public String getKafkaProducerType() {
        return kafkaProducerType;
    }

    public int getKafkaBatchSize() {
        return kafkaBatchSize;
    }

    public int getKafkaBatchMaxWaitMs() {
        return kafkaBatchMaxWaitMs;
    }

    public String getAmqpPassword() {
        return amqpPassword;
    }

    public String getAmqpUsername() {
        return amqpUsername;
    }

    public String getAmqpVirtualHost() {
        return amqpVhost;
    }

    public int getAmqpPort() {
        return amqpPort;
    }

    public String getAmqpHostname() {
        return amqpHostname;
    }

    public String getAmqpQueueName() {
        return amqpQueueName;
    }

    public String getAmqpQueueType() {
        return amqpQueueType;
    }

    public String getAmqpExchangeName() {
        return amqpExchangeName;
    }

    public String getAmqpRoutingKey() {
        return amqpRoutingKey;
    }

    public int getAmqpParallelQueues() {
        return amqpParallelQueues;
    }
}
