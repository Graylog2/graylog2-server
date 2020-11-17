/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PersistedServiceImpl;

/**
 *  @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class SystemSetting extends PersistedServiceImpl {
    
    private static final String COLLECTION_NAME = "system_settings";

    public SystemSetting(MongoConnection mongoConnection) {
        super(mongoConnection);
    }
    
    public boolean getBoolean(String key) {
        DBCollection coll = getCollection();
        
        DBObject query = new BasicDBObject();
        query.put("key", key);
        
        DBObject result = coll.findOne(query);
        if (result == null) {
            return false;
        }
        
        if (result.get("value").equals(true)) {
            return true;
        }
        
        return false;
    }
    
    public BasicDBList getList(String key) {
        DBCollection coll = getCollection();
        DBObject query = new BasicDBObject();
        query.put("key", key);

        DBObject result = coll.findOne(query);

        return (BasicDBList) result.get("value");
    }

    private DBCollection getCollection() {
        return mongoConnection.getDatabase().getCollection(COLLECTION_NAME);
    }
    
}
