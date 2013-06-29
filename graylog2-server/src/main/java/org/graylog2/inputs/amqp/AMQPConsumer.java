/**
 * Copyright 2012, 2013 Lennart Koopmann <lennart@socketfeed.com>
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

import com.codahale.metrics.Meter;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.graylog2.Core;
import org.graylog2.activities.Activity;
import org.graylog2.gelf.GELFMessage;
import org.graylog2.gelf.GELFProcessor;
import org.graylog2.inputs.syslog.SyslogProcessor;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class AMQPConsumer implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(AMQPConsumer.class);

    private Core server;
    private GELFProcessor gelfProcessor;
    private SyslogProcessor syslogProcessor;
    
    AMQPQueueConfiguration queueConfig;
    
    Connection connection;
    Channel channel;
    ExecutorService executor;
    
    private final Meter handledMessages;
    private final Meter handledSyslogMessages;
    private final Meter handledGELFMessages;
    private final Meter reQueuedMessages;
    
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
            default:
            	LOG.error("Unknown input type {} for queue {}", queueConfig.getInputType(), queueConfig.getQueueName());
            	break;
        }

        this.handledMessages = server.metrics().meter(name(AMQPConsumer.class, "handledMessages"));
        this.handledSyslogMessages = server.metrics().meter(name(AMQPConsumer.class, "handledSyslogMessages"));
        this.handledGELFMessages = server.metrics().meter(name(AMQPConsumer.class, "handledGELFMessages"));
        this.reQueuedMessages = server.metrics().meter(name(AMQPConsumer.class, "reQueuedMessages"));
    }
    
    @Override
    public void run() {
        String msg = "Setting up AMQP connection to <" + queueConfig + ">";
        LOG.info(msg);
        server.getActivityWriter().write(new Activity(msg, AMQPConsumer.class));
        listen();
    }

    public void listen() {
        try {
            channel = connect();

            // Queue config.
            Map<String, Object> arguments = new HashMap<String, Object>();
            boolean isDurable = false;
            boolean isExclusive = false;
            boolean isAutoDelete = false;
            arguments.put("x-message-ttl", queueConfig.getTtl()); // 15 minutes.

            // Automatically re-connect.
            this.connection.addShutdownListener(new AMQPReconnector(server, queueConfig));

            // Declare and bind queue.
            channel.queueDeclare(queueConfig.getQueueName(), isDurable, isExclusive, isAutoDelete, arguments);
            channel.queueBind(queueConfig.getQueueName(), queueConfig.getExchange(), queueConfig.getRoutingKey());

            consume();

            String msg = "Connected to broker <" + queueConfig + ">";
            LOG.info(msg);
            server.getActivityWriter().write(new Activity(msg, AMQPConsumer.class));
        } catch(IOException e) {
           String msg = "IO error on broker <" + queueConfig + ">. ("+ e.getMessage() + ")";
           LOG.error(msg, e);
           server.getActivityWriter().write(new Activity(msg, AMQPConsumer.class));
           
           disconnect();
        }
    }
    
    public void deleteQueueAndDisconnect() {
        String msg = "Attempting to delete and disconnect from queue [" + queueConfig.getQueueName() + "]";
        LOG.debug(msg);
        server.getActivityWriter().write(new Activity(msg, AMQPConsumer.class));
        
        try {
            channel.queueDelete(queueConfig.getQueueName());
        } catch(IOException e) {
            String msg2 = "Could not delete queue [" + queueConfig.getQueueName() + "]";
            LOG.error(msg2);
            server.getActivityWriter().write(new Activity(msg2, AMQPConsumer.class));
        }
        
        disconnect();
    }

    public void disconnect() {
        try {
            AMQPInput.getConsumers().remove(queueConfig.getId());
            
            channel.close();
        } catch (AlreadyClosedException ignore) {
            // do nothing
        } catch(IOException e) {
            LOG.error("Could not disconnect from AMQP broker!", e);
        } finally {
            if (executor != null) {
                executor.shutdownNow();
                executor = null;
            }
            try {
                if (connection != null && connection.isOpen()) {
                    connection.close();
                }
            } catch (AlreadyClosedException ignore) {
                // do nothing
            } catch (IOException e) {
                LOG.error("Could not disconnect from AMQP broker!", e);
            }
        }
    }

    public void consume() throws IOException {
        boolean autoAck = false;

        channel.basicConsume(queueConfig.getQueueName(), autoAck, createConsumer(channel));
    }

    private Channel connect() throws IOException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername(server.getConfiguration().getAmqpUsername());
        factory.setPassword(server.getConfiguration().getAmqpPassword());
        factory.setVirtualHost(server.getConfiguration().getAmqpVirtualhost());
        factory.setHost(server.getConfiguration().getAmqpHost());
        factory.setPort(server.getConfiguration().getAmqpPort());

        executor = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder()
                    .setNameFormat("amqp-consumer-" + queueConfig.getId() + "-%d")
                    .build());
        connection = factory.newConnection(executor);
        
        return connection.createChannel();
    }
    
    public Consumer createConsumer(final Channel channel) {
    	 return new DefaultConsumer(channel) {
             @Override
             public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                try {
                    // The duplication here is a bit unfortunate. Improve by having a Processor Interface.
                    switch (queueConfig.getInputType()) {
                        case GELF:
                            GELFMessage gelf = new GELFMessage(body);
                            try {
                               gelfProcessor.messageReceived(gelf);
                            } catch (BufferOutOfCapacityException e) {
                                LOG.warn("ProcessBufferProcessor is out of capacity. Requeuing message!");
                                channel.basicReject(envelope.getDeliveryTag(), true);
                                reQueuedMessages.mark();
                                return;
                            }
                             
                            handledGELFMessages.mark();
                            break;
                         case SYSLOG:
                            try {
                                syslogProcessor.messageReceived(new String(body), connection.getAddress());
                             } catch (BufferOutOfCapacityException e) {
                                LOG.warn("ProcessBufferProcessor is out of capacity. Requeuing message!");
                                channel.basicReject(envelope.getDeliveryTag(), true);
                                reQueuedMessages.mark();
                                return;
                             }

                             handledSyslogMessages.mark();
                             break;
                         default:
                        	 LOG.error("Unknown input type {} for queue {}", queueConfig.getInputType(), queueConfig.getQueueName());
                     }
                     
                     channel.basicAck(envelope.getDeliveryTag(), false);
                     handledMessages.mark();
                 } catch(Exception e) {
                	 // If something breaks here it is extremely likely that it won't work next time. Not-Ack the message and do not requeue.
                	 channel.basicNack(envelope.getDeliveryTag(), false, false); // YOLO
                     LOG.error("Could not handle message from AMQP.", e);
                 }
             }
    	 };
    }
    
    public String getQueueName() {
        return queueConfig.getQueueName();
    }
}
