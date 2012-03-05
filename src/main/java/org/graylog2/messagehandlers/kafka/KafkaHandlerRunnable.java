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

import java.io.IOException;
import java.net.InetAddress;
import java.util.zip.DataFormatException;

import org.apache.log4j.Logger;
import org.graylog2.messagehandlers.gelf.InvalidGELFCompressionMethodException;
import org.graylog2.messagehandlers.gelf.SimpleGELFClientHandler;
import org.graylog2.messagehandlers.syslog.SyslogEventHandler;
import org.productivity.java.syslog4j.server.impl.event.SyslogServerEvent;

import kafka.consumer.KafkaMessageStream;
import kafka.message.Message;

public final class KafkaHandlerRunnable implements Runnable {

    private static final Logger LOG;

    static {
        LOG = Logger.getLogger(KafkaHandler.class);
    }

    private final KafkaMessageStream stream;
    private final String type;

    public KafkaHandlerRunnable(KafkaMessageStream stream, String type) {
        this.stream = stream;
        this.type = type;
    }

    @Override
    public void run() {
        for(Message message : stream) {
            if(message.isValid()) {
                try {
                    handle(kafka.utils.Utils$.MODULE$.toString(message.payload(), "UTF-8"));
                } catch (Exception e) {
                    LOG.error("Could not handle " + type + " message from Kafka: ", e);
                }
            }
        }
    }

    private void handle(String message) throws IOException, DataFormatException, InvalidGELFCompressionMethodException {
        if(type.equals(KafkaHandler.TYPE_SYSLOG)) {
            LOG.debug("Handling a syslog message from Kafka.");
            SyslogServerEvent syslogMessage = 
                    new SyslogServerEvent(message.getBytes(), message.length(), InetAddress.getLocalHost());
            SyslogEventHandler syslogHandler = new SyslogEventHandler();
            syslogHandler.event(null, null, syslogMessage);
        } else if(type.equals(KafkaHandler.TYPE_GELF)) {
            LOG.debug("Handling a GELF message from Kafka.");
            SimpleGELFClientHandler gelfHandler = new SimpleGELFClientHandler(message);
            gelfHandler.handle();
        } else {
            LOG.warn("Unknown message type " + type + " for kafka handler.");
        }
    }
}
