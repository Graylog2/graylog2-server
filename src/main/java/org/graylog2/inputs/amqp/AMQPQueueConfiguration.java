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

import com.google.common.collect.Sets;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import java.util.Set;
import java.util.UUID;
import org.apache.log4j.Logger;
import org.graylog2.Core;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class AMQPQueueConfiguration {
    
    private static final Logger LOG = Logger.getLogger(AMQPQueueConfiguration.class);
    
    public enum InputType { GELF, SYSLOG, UNKNOWN }
    
    private String exchange;
    private String routingKey;
    private InputType inputType;
    private String gl2NodeId;
    
    private String queueName;
    
    public static Set<AMQPQueueConfiguration> fetchAll(Core server) {
        Set<AMQPQueueConfiguration> configs = Sets.newHashSet();

        DBCollection coll = server.getMongoConnection().getDatabase().getCollection("amqp_configurations");
        DBObject query = new BasicDBObject();
        query.put("disabled", new BasicDBObject("$ne", true));
        DBCursor cur = coll.find(query);

        while (cur.hasNext()) {
            try {
                DBObject config = cur.next();

                configs.add(new AMQPQueueConfiguration(
                            (String) config.get("exchange"),
                            (String) config.get("routing_key"),
                            inputTypeFromString((String) config.get("input_type")),
                            server.getServerId()
                        ));
            } catch (Exception e) {
                LOG.warn("Can't fetch AMQP queue config. Skipping. " + e.getMessage(), e);
            }
        }

        return configs;
    }
    
    public AMQPQueueConfiguration(String exchange, String routingKey, InputType inputType, String gl2NodeId) {
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.inputType = inputType;
        this.gl2NodeId = gl2NodeId;
        
        queueName = generateQueueName();
    }
    
    public String getExchange() {
        return exchange;
    }
    
    public String getRoutingKey() {
        return routingKey;
    }
    
    public InputType getInputType() {
        return inputType;
    }
    
    public String getQueueName() {
        return queueName;
    }
    
    public static InputType inputTypeFromString(String type) {
        if (type.equals("gelf")) {
            return InputType.GELF;
        }
        
        if (type.equals("syslog")) {
            return InputType.SYSLOG;
        }
        
        return InputType.UNKNOWN;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(exchange).append(":")
                .append(routingKey).append(":")
                .append(inputType);

        return sb.toString();
    }
    
    private String generateQueueName() {
        StringBuilder sb = new StringBuilder();
        sb.append("gl2-").append(gl2NodeId).append("-q-")
                .append(inputType.toString().toLowerCase())
                .append("-").append(UUID.randomUUID().toString());

        return sb.toString();
    }
    
}
