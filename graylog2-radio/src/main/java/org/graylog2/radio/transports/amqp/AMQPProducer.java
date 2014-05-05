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

import org.graylog2.plugin.Message;
import org.graylog2.radio.Radio;
import org.graylog2.radio.transports.RadioTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class AMQPProducer implements RadioTransport {

    private static final Logger LOG = LoggerFactory.getLogger(AMQPProducer.class);

    public final static String EXCHANGE = "graylog2";
    public final static String QUEUE = "graylog2-radio-messages";
    public final static String ROUTING_KEY = "graylog2-radio-message";

    private final AMQPSender sender;

    public AMQPProducer(Radio radio) {
        sender = new AMQPSender(
                radio.getConfiguration().getAmqpHostname(),
                radio.getConfiguration().getAmqpPort(),
                radio.getConfiguration().getAmqpVirtualHost(),
                radio.getConfiguration().getAmqpUsername(),
                radio.getConfiguration().getAmqpPassword()
        );
    }

    @Override
    public void send(Message msg) {
        try {
            sender.send(msg);
        } catch (IOException e) {
            LOG.error("Could not write to AMQP.", e);
        }
    }

}
