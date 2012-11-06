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
package org.graylog2.users;

import com.beust.jcommander.internal.Maps;
import com.google.common.collect.Sets;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.graylog2.Core;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class User {
   
    private static final Logger LOG = Logger.getLogger(User.class);
    
    private final ObjectId id;
    private final String login;
    private final String name;
    private final Map<String, String> transports;
    
    private final DBObject mongoObject;
    
    public User (DBObject user) {
        this.id = (ObjectId) user.get("_id");
        this.login = (String) user.get("login");
        this.name = (String) user.get("name");
        this.transports = Maps.newHashMap();
        
        
        if (user.get("transports") != null) {
            BasicDBList transportList = (BasicDBList) user.get("transports");
            
            if (transportList != null && transportList.size() > 0) {
                for (Object transportObj : transportList) {
                    DBObject tp = (BasicDBObject) transportObj;
                    transports.put((String) tp.get("typeclass"), (String) tp.get("value"));
                }
            }
        }
        
        this.mongoObject = user;
    }
    
    public static Set<User> fetchAll(Core server) {
        Map<String, Object> emptyMap = Maps.newHashMap();
        return fetchAll(server, emptyMap);
    }
    
    public static Set<User> fetchAll(Core server, Map<String, Object> additionalQueryOpts) {
        UserCache cache = UserCache.getInstance();
        if (cache.valid()) {
            return cache.get();
        }

        Set<User> users = Sets.newHashSet();

        DBCollection coll = server.getMongoConnection().getDatabase().getCollection("users");
        DBObject query = new BasicDBObject();
        
        // query.putAll() is not working
        for (Map.Entry<String, Object> o : additionalQueryOpts.entrySet()) {
             query.put(o.getKey(), o.getValue());
        }
            
        DBCursor cur = coll.find(query);

        while (cur.hasNext()) {
            try {
                users.add(new User(cur.next()));
            } catch (Exception e) {
                LOG.warn("Can't fetch user. Skipping. " + e.getMessage(), e);
            }
        }

        cache.set(users);

        return users;
    }

    public ObjectId getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getTransports() {
        return transports;
    }
    
}
