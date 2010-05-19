/**
 * MongoMapper.java: lennart | Apr 13, 2010 9:13:03 PM
 */

package graylog2.database;

import com.mongodb.Mongo;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;

import org.productivity.java.syslog4j.server.SyslogServerEventIF;

public class MongoMapper {
    // TODO: make configurable
    public static final String MONGO_HOSTNAME = "localhost";
    public static final int MONGO_PORT = 27017;
    public static final String MONGO_DB_NAME = "graylog2";
    public static final int MAX_MESSAGE_SIZE = 500000000;

    private Mongo m = null;
    private DB db = null;

    private boolean connect() {
        try {
            this.m = new Mongo(MongoMapper.MONGO_HOSTNAME, MongoMapper.MONGO_PORT);
            this.db = m.getDB(MongoMapper.MONGO_DB_NAME);
        } catch (java.net.UnknownHostException e) {
            return false;
        }

        return true;
    }

    public boolean insert(SyslogServerEventIF event) {
        try {
            if (!this.connect()) {
                return false;
            }

            DBCollection coll = null;

            // Create a capped collection
            if(db.getCollectionNames().contains("messages")) {
                coll = db.getCollection("messages");
            } else {
                coll = db.createCollection("messages", BasicDBObjectBuilder.start().add("capped", true).add("size", MongoMapper.MAX_MESSAGE_SIZE).get());
            }

            BasicDBObject dbObj = new BasicDBObject();
            dbObj.put("message", event.getMessage());
            dbObj.put("date", event.getDate());
            dbObj.put("host", event.getHost());
            dbObj.put("facility", event.getFacility());
            dbObj.put("level", event.getLevel());

            // Inserto BasicDBObject into DBCollection.
            coll.insert(dbObj);
        } catch(Exception e) {
            return false;
        }

        return true;
    }

}
