/**
 * Copyright 2014 Lennart Koopmann <lennart@torch.sh>
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
package org.graylog2.radio.transports.amqp;

import com.rabbitmq.client.*;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.RadioMessage;
import org.msgpack.MessagePack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class AMQPSender {

    private static final Logger LOG = LoggerFactory.getLogger(AMQPSender.class);

    // Not threadsafe!

    private final String hostname;
    private final int port;
    private final String vHost;
    private final String username;
    private final String password;

    private Connection connection;
    private Channel channel;

    private final MessagePack pack;

    public AMQPSender(String hostname, int port, String vHost, String username, String password) {
        pack = new MessagePack();

        this.hostname = hostname;
        this.port = port;
        this.vHost = vHost;
        this.username = username;
        this.password = password;
    }

    public void send(Message msg) throws IOException {
        if (!isConnected()) {
            connect();
        }

        byte[] body = RadioMessage.serialize(pack, msg);

        boolean mandatory = true;
        channel.basicPublish(AMQPProducer.EXCHANGE, AMQPProducer.ROUTING_KEY, mandatory, new AMQP.BasicProperties(), body);
    }

    public void connect() throws IOException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(hostname);
        factory.setPort(port);

        factory.setVirtualHost(vHost);

        // Authenticate?
        if(username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            factory.setUsername(username);
            factory.setPassword(password);
        }

        connection = factory.newConnection();
        channel = connection.createChannel();

        // It's ok if the queue or exchange already exist.
        channel.queueDeclare(AMQPProducer.QUEUE, true, false, false, null);
        channel.exchangeDeclare(AMQPProducer.EXCHANGE, "topic", true, false, null);

        channel.queueBind(AMQPProducer.QUEUE, AMQPProducer.EXCHANGE, AMQPProducer.ROUTING_KEY);
    }

    public boolean isConnected() {
        return connection != null
                && connection.isOpen()
                && channel != null
                && channel.isOpen();
    }

    public void close() throws IOException {
        if (channel != null && channel.isOpen()) {
            channel.close();
        }

        if (connection != null && connection.isOpen()) {
            connection.close();
        }
    }

}
