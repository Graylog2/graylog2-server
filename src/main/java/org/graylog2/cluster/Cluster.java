/**
 * Copyright 2010, 2011, 2012 Lennart Koopmann <lennart@socketfeed.com>
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
package org.graylog2.cluster;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import java.util.List;
import java.util.Set;
import org.graylog2.Core;
import org.graylog2.plugin.Tools;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class Cluster {
 
    public static final int PING_TIMEOUT = 7;
    
    Core localServer;
    
    public Cluster(Core server) {
        this.localServer = server;
    }

    public List<Node> getActiveNodes() {
        List<Node> nodes = Lists.newArrayList();
        
        // Now construct a Node from every node_id.
        for (String nodeId : getActiveNodeIds()) {
            Node node = new Node();
            node.setId(nodeId);
            node.setIsMaster(getBooleanNodeAttribute(nodeId, "is_master"));

            nodes.add(node);
        }
 
        return nodes;
    }
    
    public Set<String> getActiveNodeIds() {
        DBCollection coll = localServer.getMongoConnection().getDatabase().getCollection("server_values");

        Set<String> nodeIds = Sets.newHashSet();
        
        BasicDBObject query = new BasicDBObject();
        query.put("type", "ping");
        query.put("value", new BasicDBObject("$gte", Tools.getUTCTimestamp()-PING_TIMEOUT));
        
        for (DBObject obj : coll.find(query)) {
            nodeIds.add((String) obj.get("server_id"));
        }
        
        return nodeIds;
    }
    
    public int getActiveNodeCount() {
        return getActiveNodeIds().size();
    }
    
    public int masterCountExcept(String exceptNode) {
        int masters = 0;
        for (Node node : getActiveNodes()) {
            if (node.isMaster() && !node.getId().equals(exceptNode)) {
                masters++;
            }
        }
        
        return masters;
    }

    private boolean getBooleanNodeAttribute(String nodeId, String key) {
        DBCollection coll = localServer.getMongoConnection().getDatabase().getCollection("server_values");

        BasicDBObject q = new BasicDBObject();
        q.put("type", key);
        q.put("server_id", nodeId);
        
        DBObject result = coll.findOne(q);
        if (result == null) {
            return false;
        }
        
        if (result.get("value").equals(true)) {
            return true;
        }
        
        return false;
    }
    
}
