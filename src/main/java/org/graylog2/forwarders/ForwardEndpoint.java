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

package org.graylog2.forwarders;

import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.forwarders.forwarders.GELFMessageForwarder;
import org.graylog2.forwarders.forwarders.LogglyForwarder;
import org.graylog2.forwarders.forwarders.UDPSyslogForwarder;

/**
 * ForwardEndpoint.java: Apr 3, 2011 11:42:58 PM
 *
 * [description]
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class ForwardEndpoint {

    public static final int ENDPOINT_TYPE_UDP_SYSLOG = 0;
    public static final int ENDPOINT_TYPE_GELF = 1;
    public static final int ENDPOINT_TYPE_LOGGLY = 2;

    private ObjectId id = null;
    private int endpointType = -1;
    private String title = null;
    private int port = 0;
    private String host = null;

    private DBObject mongoObject = null;

    public ForwardEndpoint(DBObject endpoint) throws InvalidEndpointTypeException {
        this.id = (ObjectId) endpoint.get("_id");
        this.title = (String) endpoint.get("title");
        this.port = (Integer) endpoint.get("port");
        this.host = (String) endpoint.get("host");
        this.endpointType = endpointTypeToNumber((String) endpoint.get("endpoint_type"));

        this.mongoObject = endpoint;
    }

    public MessageForwarderIF getForwarder() throws InvalidEndpointTypeException {
        switch (endpointType) {
            case ENDPOINT_TYPE_UDP_SYSLOG:
                return new UDPSyslogForwarder(this.getHost(), this.getPort());
            case ENDPOINT_TYPE_GELF:
                return new GELFMessageForwarder(this.getHost(), this.getPort());
            case ENDPOINT_TYPE_LOGGLY:
                return new LogglyForwarder(this.getHost());
        }

        throw new InvalidEndpointTypeException();
    }

    public String getHumanReadableEndpointType() throws InvalidEndpointTypeException {
        return endpointTypeToHuman(this.getEndpointType());
    }

    /**
     * @return the id
     */
    public ObjectId getId() {
        return id;
    }

    /**
     * @return the endpointType
     */
    public int getEndpointType() {
        return endpointType;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    public static int endpointTypeToNumber(String endpointType) throws InvalidEndpointTypeException {
        // lol no switch for strings.
        if(endpointType.equals("syslog")) {
            return ENDPOINT_TYPE_UDP_SYSLOG;
        } else if(endpointType.equals("gelf")) {
            return ENDPOINT_TYPE_GELF;
        } else if(endpointType.equals("loggly")) {
            return ENDPOINT_TYPE_LOGGLY;
        }

        throw new InvalidEndpointTypeException();
    }

    public static String endpointTypeToHuman(int endpointType) throws InvalidEndpointTypeException {
        switch (endpointType) {
            case ENDPOINT_TYPE_UDP_SYSLOG:
                return "syslog";
            case ENDPOINT_TYPE_GELF:
                return "gelf";
            case ENDPOINT_TYPE_LOGGLY:
                return "loggly";
        }

        throw new InvalidEndpointTypeException();
    }

}