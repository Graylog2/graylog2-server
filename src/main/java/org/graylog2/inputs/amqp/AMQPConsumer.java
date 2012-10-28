/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
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
package org.graylog2.inputs.amqp;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.log4j.Logger;
import org.graylog2.Core;
import org.graylog2.gelf.GELFMessage;
import org.graylog2.gelf.GELFProcessor;
import org.graylog2.inputs.syslog.SyslogProcessor;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class AMQPConsumer implements Runnable {

    private static final Logger LOG = Logger.getLogger(AMQPConsumer.class);

    private Core server;
    private GELFProcessor gelfProcessor;
    private SyslogProcessor syslogProcessor;
    
    AMQPQueueConfiguration queueConfig;
    
    Connection connection;
    Channel channel;
    
    Meter handledMessages = Metrics.newMeter(AMQPConsumer.class, "HandledAMQPMessages", "messages", TimeUnit.SECONDS);

    public AMQPConsumer(Core server, AMQPQueueConfiguration queueConfig) {
        this.server = server;
        this.queueConfig = queueConfig;
        
        switch (queueConfig.getInputType()) {
            case GELF:
                this.gelfProcessor = new GELFProcessor(server);
                break;
            case SYSLOG:
                this.syslogProcessor = new SyslogProcessor(server);
                break;
        }
    }
    
    @Override
    public void run() {
        LOG.info("Setting up AMQP connection to <" + queueConfig + ">");
        listen();
    }

    public void listen() {
        try {
            channel = connect();

            // Queue config.
            Map<String, Object> arguments = new HashMap<String, Object>();
            boolean isDurable = false;
            boolean isExclusive = false;
            boolean isAutoDelete = true;
            arguments.put("x-message-ttl", queueConfig.getTtl()); // 15 minutes.

            // Automatically re-connect.
            this.connection.addShutdownListener(new AMQPReconnector(server, queueConfig));

            // Declare and bind queue.
            channel.queueDeclare(queueConfig.getQueueName(), isDurable, isExclusive, isAutoDelete, arguments);
            channel.queueBind(queueConfig.getQueueName(), queueConfig.getExchange(), queueConfig.getRoutingKey());

            consume();

            LOG.info("Connected to broker <" + queueConfig + ">");
        } catch(IOException e) {
           LOG.error("IO error on broker <" + queueConfig + ">", e);
        }
    }

    public void disconnect() {
        try {
            channel.queueDelete(queueConfig.getQueueName());
            channel.close();
            connection.close();
        } catch(IOException e) {
            LOG.error("Could not disconnect from AMQP broker!", e);
        }
    }

    public void consume() throws IOException {
        boolean autoAck = true;
        channel.basicConsume(queueConfig.getQueueName(), autoAck,
            new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    try {
                        handledMessages.mark();

                        switch (queueConfig.getInputType()) {
                            case GELF:
                                GELFMessage gelf = new GELFMessage(body);
                                gelfProcessor.messageReceived(gelf);
                                break;
                            case SYSLOG:
                                syslogProcessor.messageReceived(new String(body), connection.getAddress());
                                break;
                        }
                    } catch(Exception e) {
                        LOG.error("Could not handle message from AMQP.", e);
                    }
                }
             }
        );
    }

    private Channel connect() throws IOException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername(server.getConfiguration().getAmqpUsername());
        factory.setPassword(server.getConfiguration().getAmqpPassword());
        factory.setVirtualHost(server.getConfiguration().getAmqpVirtualhost());
        factory.setHost(server.getConfiguration().getAmqpHost());
        factory.setPort(server.getConfiguration().getAmqpPort());

        connection = factory.newConnection(Executors.newCachedThreadPool(
            new BasicThreadFactory.Builder()
                .namingPattern("amqp-consumer-" + queueConfig.getId() + "-%d")
                .build()
        ));
        
        return connection.createChannel();
    }
    
    public String getQueueName() {
        return queueConfig.getQueueName();
    }
    
}
