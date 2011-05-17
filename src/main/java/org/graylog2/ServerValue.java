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

package org.graylog2;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.graylog2.database.MongoBridge;
import org.graylog2.database.MongoConnection;

/**
 * ServerValue.java: Jan 16, 2011 1:35:00 PM
 *
 * Filling the server_values collection
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class ServerValue {

    DBObject mongoInstance = null;

    public ServerValue(String instanceId) {
        this.removeDeadInstances();
        
        this.mongoInstance = findOrCreateMongoInstance(instanceId);
        System.out.println("GOT: " + this.mongoInstance.toString());
    }

    private void removeDeadInstances() {
        System.out.println("PLEASE IMPLEMENT ME - REMOVE DEAD INSTANCES");
    }

    private DBObject findOrCreateMongoInstance(String instanceId) {
        DBCollection sv = MongoConnection.getInstance().getDatabase().getCollection("server_values");

        // Doing it this way because java driver sucks in querying for subdoc keys. :(
        DBCursor cur = sv.find(); // find all
        while (cur.hasNext()) {
            DBObject obj = cur.next();
            if (obj.containsField(instanceId)) {
                // Found the instance.
                return obj;
            }
        }

        // There is no such instance in the collection yet if we arrive here.
        DBObject instance = new BasicDBObject();
        instance.put(instanceId, null);
        sv.insert(instance);

        return instance;
    }






    public static void setStartupTime(int timestamp) {
        set("startup_time", timestamp);
    }

    public static void setPID(int pid) {
        set("pid", pid);
    }

    public static void setJREInfo(String info) {
        set("jre", info);
    }

    public static void setAvailableProcessors(int processors) {
        set("available_processors", processors);
    }

    public static void setGraylog2Version(String version) {
        set("graylog2_version", version);
    }

    public static void setLocalHostname(String hostname) {
        set("local_hostname", hostname);
    }

    public static void writeThroughput(int current, int highest) {
        MongoBridge m = new MongoBridge();
        m.writeThroughput(current, highest);
    }

    private static void set(String key, Object value) {
        MongoBridge m = new MongoBridge();
        m.setSimpleServerValue(key, value);
    }

}