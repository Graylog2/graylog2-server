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
 * AMQPSubscribedExchange.java: Jan 21, 2011 7:52:29 PM
 *
 * Representing an AMQP topic exchange to subscribe to.
 *
 * @author: Dick Davies <rasputnik@hellooperator.net>
 */
public final class AMQPSubscribedExchange {

    public static final int TYPE_SYSLOG = 0;
    public static final int TYPE_GELF = 1;

    private String name = null;
    private String routingKey = null;
    private int type = -1;

    public AMQPSubscribedExchange(String name, String routingKey, String type) throws InvalidExchangeTypeException {
        setName(name);
        setRoutingKey(routingKey);
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
     * @return the routingKey
     */
    public String getRoutingKey() {
        return routingKey;
    }

    /**
     * @param routingKey the routingKey to set
     */
    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
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
    public void setType(String type) throws InvalidExchangeTypeException {
        if (type.equals("syslog")) {
            this.type = TYPE_SYSLOG;
        } else if(type.equals("gelf")) {
            this.type = TYPE_GELF;
        } else {
            throw new InvalidExchangeTypeException();
        }
    }

}
