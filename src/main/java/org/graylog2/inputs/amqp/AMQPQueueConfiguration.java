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
import org.bson.types.ObjectId;
import org.graylog2.Core;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class AMQPQueueConfiguration {
    
    private static final Logger LOG = Logger.getLogger(AMQPQueueConfiguration.class);
    
    public enum InputType { GELF, SYSLOG, UNKNOWN }
    
    private ObjectId id;
    private String exchange;
    private String routingKey;
    private int ttl;
    private InputType inputType;
    private String gl2NodeId;
    private String uuid;
    
    private String queueName;
    
    public static Set<AMQPQueueConfiguration> fetchAll(Core server) {
        Set<AMQPQueueConfiguration> configs = Sets.newHashSet();

        DBCollection coll = server.getMongoConnection().getDatabase().getCollection("amqp_configurations");
        DBCursor cur = coll.find(new BasicDBObject());

        while (cur.hasNext()) {
            try {
                DBObject config = cur.next();

                configs.add(new AMQPQueueConfiguration(
                            (ObjectId) config.get("_id"),
                            (String) config.get("exchange"),
                            (String) config.get("routing_key"),
                            (Integer) config.get("ttl"),
                            inputTypeFromString((String) config.get("input_type")),
                            server.getServerId()
                        ));
            } catch (Exception e) {
                LOG.warn("Can't fetch AMQP queue config. Skipping. " + e.getMessage(), e);
            }
        }

        return configs;
    }
    
    public static Set<String> fetchAllIds(Core server) {
        Set<String> configIDs = Sets.newHashSet();
        
        for (AMQPQueueConfiguration config : fetchAll(server)) {
            configIDs.add(config.getId());
        }
                
        return configIDs;
    }
    
    public AMQPQueueConfiguration(ObjectId id, String exchange, String routingKey, int ttl, InputType inputType, String gl2NodeId) {
        this.id = id;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.ttl = ttl;
        this.inputType = inputType;
        this.gl2NodeId = gl2NodeId;
        
        queueName = generateQueueName();
    }
    
    public String getId() {
        return id.toString();
    }
    
    public String getExchange() {
        return exchange;
    }
    
    public String getRoutingKey() {
        return routingKey;
    }
    
    public int getTtl() {
        return ttl;
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
        sb.append(exchange).append(":")
                .append(routingKey).append(":")
                .append(inputType);

        return sb.toString();
    }
    
    private String generateQueueName() {
        StringBuilder sb = new StringBuilder();
        sb.append("gl2-").append(inputType.toString().toLowerCase())
                .append("-").append(exchange).append("-")
                .append(id.toString());

        return sb.toString();
    }
    
}
