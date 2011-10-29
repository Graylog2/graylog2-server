/**
 * Copyright 2011 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.messagehandlers.syslog;

import java.net.InetAddress;
import org.productivity.java.syslog4j.server.SyslogServerEventIF;
import org.productivity.java.syslog4j.server.impl.event.SyslogServerEvent;

/**
 * GraylogSyslogServerEvent.java: Jan 23, 2011 12:01:25 AM
 *
 * Extends the Syslog4j SyslogServerEvent with a AMQP receiver queue field.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class GraylogSyslogServerEvent extends SyslogServerEvent implements SyslogServerEventIF {

    private String amqpReceiverQueue = null;

    public GraylogSyslogServerEvent(byte[] message, int messageLength, InetAddress addr) {
        super(message, messageLength, addr);
    }

    /**
     * @return the amqpReceiverQueue
     */
    public String getAmqpReceiverQueue() {
        return this.amqpReceiverQueue;
    }

    /**
     * @param amqpReceiverQueue the amqpReceiverQueue to set
     */
    public void setAmqpReceiverQueue(String amqpQueue) {
        this.amqpReceiverQueue = amqpQueue;
    }

}