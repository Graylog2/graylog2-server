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

package org.graylog2.messagehandlers.amqp;

/**
 * AMQPSubscribedQueue.java: Jan 21, 2011 7:52:29 PM
 *
 * Representing an AMQP queue to subscribe to.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public final class AMQPSubscribedQueue {

    public static final int TYPE_SYSLOG = 0;
    public static final int TYPE_GELF = 1;

    private String name = null;
    private int type = -1;

    public AMQPSubscribedQueue(String name, String type) throws InvalidQueueTypeException {
        setName(name);
        setType(type);
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * TYPE_SYSLOG or TYPE_GELF
     *
     * @return the type
     */
    public int getType() {
        return type;
    }

    /**
     * TYPE_SYSLOG or TYPE_GELF
     * 
     * @param type the type to set
     */
    public void setType(String type) throws InvalidQueueTypeException {
        if (type.equals("syslog")) {
            this.type = TYPE_SYSLOG;
        } else if(type.equals("gelf")) {
            this.type = TYPE_GELF;
        } else {
            throw new InvalidQueueTypeException();
        }
    }

}
