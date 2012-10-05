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
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import java.util.List;
import org.graylog2.Core;
import org.graylog2.Tools;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class Cluster {
 
    private static final int PING_TIMEOUT = 7;
    
    Core localServer;
    
    public Cluster(Core server) {
        this.localServer = server;
    }

    public List<String> getNodes() {
        DBCollection coll = localServer.getMongoConnection().getDatabase().getCollection("server_values");
        
        List<String> nodes = Lists.newArrayList();
        
        BasicDBObject query = new BasicDBObject();
        query.put("type", "ping");
        query.put("value", new BasicDBObject("$gte", Tools.getUTCTimestamp()-PING_TIMEOUT));
        
        for (DBObject obj : coll.find(query)) {
            nodes.add((String) obj.get("server_id"));
        }
        
        return nodes;
    }    

}
