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
package org.graylog2.radio;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.validators.InetPortValidator;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.lmax.disruptor.*;
import org.graylog2.plugin.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Configuration {

    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

    public enum TRANSPORT_TYPE {
        AMQP, KAFKA
    }

    @Parameter(value = "node_id_file", required = false)
    private String nodeIdFile = "/etc/graylog2-radio-node-id";

    @Parameter(value = "transport_type", required = true)
    private String transportType = "amqp";

    @Parameter(value = "rest_listen_uri", required = true)
    private String restListenUri = "http://127.0.0.1:12950/";

    @Parameter(value = "graylog2_server_uri", required = true)
    private String graylog2ServerUri;

    @Parameter(value = "rest_transport_uri", required = false)
    private String restTransportUri;

    @Parameter(value = "kafka_brokers", required = false)
    private String kafkaBrokers;

    @Parameter(value = "kafka_required_acks", required = false)
    private int kafkaRequiredAcks = 1;

    @Parameter(value = "kafka_producer_type", required = false)
    private String kafkaProducerType = "async";

    @Parameter(value = "kafka_batch_size", required = false)
    private int kafkaBatchSize = 200;

    @Parameter(value = "kafka_batch_max_wait_ms", required = false)
    private int kafkaBatchMaxWaitMs = 250;

    @Parameter(value = "amqp_broker_hostname", required = false)
    private String amqpHostname = "localhost";

    @Parameter(value = "amqp_broker_port", required = false, validator = InetPortValidator.class)
    private int amqpPort = 5672;

    @Parameter(value = "amqp_broker_username", required = false)
    private String amqpUsername;

    @Parameter(value = "amqp_broker_password", required = false)
    private String amqpPassword;

    @Parameter(value = "amqp_broker_vhost", required = false)
    private String amqpVhost = "/";

    @Parameter(value = "ring_size", required = true, validator = PositiveIntegerValidator.class)
    private int ringSize = 1024;

    @Parameter(value = "processbuffer_processors", required = true, validator = PositiveIntegerValidator.class)
    private int processBufferProcessors = 5;

    @Parameter(value = "processor_wait_strategy", required = true)
    private String processorWaitStrategy = "blocking";


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
        return Tools.getUriStandard(restListenUri);
    }

    public URI getRestTransportUri() {
        if (restTransportUri == null || restTransportUri.isEmpty()) {
            return null;
        }

        return Tools.getUriStandard(restTransportUri);
    }

    public URI getDefaultRestTransportUri() {
        URI transportUri;
        URI listenUri = getRestListenUri();

        if (listenUri.getHost().equals("0.0.0.0")) {
            String guessedIf;
            try {
                guessedIf = Tools.guessPrimaryNetworkAddress().getHostAddress();
            } catch (Exception e) {
                LOG.error("Could not guess primary network address for rest_transport_uri. Please configure it in your graylog2.conf.", e);
                throw new RuntimeException("No rest_transport_uri.");
            }

            String transportStr = "http://" + guessedIf + ":" + listenUri.getPort();
            transportUri = Tools.getUriStandard(transportStr);
        } else {
            transportUri = listenUri;
        }

        return transportUri;
    }

    public URI getGraylog2ServerUri() {
        if (graylog2ServerUri == null || graylog2ServerUri.isEmpty()) {
            return null;
        }

        return Tools.getUriStandard(graylog2ServerUri);
    }

    public void setRestTransportUri(String restTransportUri) {
        this.restTransportUri = restTransportUri;
    }

    public int getRingSize() {
        return ringSize;
    }

    public WaitStrategy getProcessorWaitStrategy() {
        if (processorWaitStrategy.equals("sleeping")) {
            return new SleepingWaitStrategy();
        }

        if (processorWaitStrategy.equals("yielding")) {
            return new YieldingWaitStrategy();
        }

        if (processorWaitStrategy.equals("blocking")) {
            return new BlockingWaitStrategy();
        }

        if (processorWaitStrategy.equals("busy_spinning")) {
            return new BusySpinWaitStrategy();
        }

        LOG.warn("Invalid setting for [processor_wait_strategy]:"
                + " Falling back to default: BlockingWaitStrategy.");
        return new BlockingWaitStrategy();
    }

    public int getProcessBufferProcessors() {
        return processBufferProcessors;
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

}
