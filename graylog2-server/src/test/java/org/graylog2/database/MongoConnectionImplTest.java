package org.graylog2.database;

import com.mongodb.MongoClientURI;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class MongoConnectionImplTest {
    @Test
    public void testConnectionString() {
        MongoClientURI mongoClientURI = new MongoClientURI("mongodb://username:secret+@localhost:27017/graylog");

        List<String> hosts = new ArrayList<>();
        hosts.add("localhost:27017");

        assertEquals(mongoClientURI.getUsername(), "username");
        assertEquals(String.valueOf(mongoClientURI.getPassword()), "secret+");
        assertEquals(mongoClientURI.getHosts(), hosts);
        assertEquals(mongoClientURI.getDatabase(), "graylog");
    }
}
