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
package org.graylog2.plugins;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import java.util.Map;
import org.apache.log4j.Logger;
import org.elasticsearch.common.collect.Maps;
import org.graylog2.Core;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class PluginConfiguration {
 
    private static final Logger LOG = Logger.getLogger(PluginConfiguration.class);
    
    public static Map<String, String> load(Core server, String className) {
        Map<String, String> configuration = Maps.newHashMap();

        try {
            DBCollection coll = server.getMongoConnection().getDatabase().getCollection("plugin_configurations");

            DBObject query = new BasicDBObject();
            query.put("typeclass", className);
      
            DBObject res = coll.findOne(query);
            
            if (res == null) {
                return configuration;
            }

            DBObject rawConfig = (BasicDBObject) res.get("configuration");
            Map<String, String> configs = rawConfig.toMap();
            
            return configs;
        } catch (Exception e) {
            LOG.error("Could not fetch plugin configuration for <" + className + ">.", e);
        }
        
        return configuration;
    }
    
}
