/**
 * MongoMapper.java: lennart | Apr 13, 2010 9:13:03 PM
 */

package graylog2.database;

import graylog2.Log;

import com.mongodb.Mongo;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;

import org.productivity.java.syslog4j.server.SyslogServerEventIF;

public class MongoMapper {
    // TODO: make configurable
    public static final int MAX_MESSAGE_SIZE = 500000000;
    public static final int STANDARD_PORT = 27017;

    private Mongo m = null;
    private DB db = null;
    
    private String username = null;
    private String password = null;
    private String hostname = null;
    private String database = null;
    private int    port = 27017;

    public MongoMapper(String username, String password, String hostname, String database, int port) {
        this.username = username;
        this.password = password;
        this.hostname = hostname;
        this.database = database;
        if (port == 0) {
            this.port = MongoMapper.STANDARD_PORT;
        } else {
            this.port = port;
        }
    }

    private void connect() throws Exception {
        this.m = new Mongo(this.hostname, this.port);
        this.db = m.getDB(this.database);

        // Try to authenticate.
        if(!db.authenticate(this.username, this.password.toCharArray())) {
            throw new Exception("Could not authenticate to database '" + this.database + "' with user '" + this.username + "'.");
        }
    }

    public boolean insert(SyslogServerEventIF event) {
        try {
            this.connect();
            
            DBCollection coll = null;

            // Create a capped collection if the collection does not yet exist.
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
            // Something failed. Log and return.
            Log.crit(e.toString());
            return false;
        }

        return true;
    }

}
