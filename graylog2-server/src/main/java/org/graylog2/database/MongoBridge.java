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

package org.graylog2.database;

import java.util.Map;
import java.util.Set;

import org.graylog2.Core;
import org.graylog2.plugin.buffers.BufferWatermark;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;


/**
 * Simple mapping methods to MongoDB.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class MongoBridge {

    private static final Logger LOG = LoggerFactory.getLogger(MongoBridge.class);
    private MongoConnection connection;
    
    Core server;

    public MongoBridge(Core server) {
        this.server = server;
    }

    public MongoConnection getConnection() {
        return connection;
    }

    public void setConnection(MongoConnection connection) {
        this.connection = connection;
    }

}
