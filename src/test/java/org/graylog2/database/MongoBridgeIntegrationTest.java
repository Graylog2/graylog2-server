/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.graylog2.database;

import com.mongodb.*;
import java.util.List;
import java.util.Properties;
import org.graylog2.Main;
import org.graylog2.messagehandlers.gelf.GELFMessage;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.productivity.java.syslog4j.server.impl.event.SyslogServerEvent;

/**
 *
 * @author lennart
 */
public class MongoBridgeIntegrationTest {

    public MongoBridgeIntegrationTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        // Required because of missing dependency injection in insertGelfMessage()
        Main.masterConfig = new Properties();
        Main.masterConfig.setProperty("messages_collection_size", "1000");

        // Connect to MongoDB (test database)
        MongoConnection.getInstance().connect(
            null,
            null,
            "localhost",
            "graylog2test",
            Integer.valueOf(27017),
            "false"
        );

        // TODO: Truncate messages collection.
        DB db = MongoConnection.getInstance().getDatabase();
        DBCollection coll = db.getCollection("messages");
        coll.drop();
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getMessagesColl method, of class MongoBridge.
     */
    @Test
    public void testGetMessagesColl() {
        MongoBridge instance = new MongoBridge();
        DBCollection coll = instance.getMessagesColl();
        assertTrue(coll.getName().equals("messages"));
    }

    /**
     * Test of insert method, of class MongoBridge.
     */
    @Test
    public void testInsert() throws Exception {
        // Build an event.
        byte[] msg = "mama".getBytes();
        SyslogServerEvent event = new SyslogServerEvent(msg, msg.length, null);
        event.setMessage("testmessage");
        event.setHost("testhost");
        event.setFacility(4);
        event.setLevel(5);

        // Insert the event.
        MongoBridge instance = new MongoBridge();
        instance.insert(event);

        // Fetch the event and compare.
        DBCollection coll = instance.getMessagesColl();
        long count = coll.getCount();
        assertTrue(count == 1);

        DBObject res = coll.findOne();
        assertEquals(res.get("message"), "testmessage");
        assertEquals(res.get("host"), "testhost");
        assertEquals(res.get("facility"), 4);
        assertEquals(res.get("level"), 5);
    }

    /**
     * Test of insertGelfMessage method, of class MongoBridge.
     */
    @Test
    public void testInsertGelfMessage() throws Exception {
        GELFMessage message = new GELFMessage();
        message.shortMessage = "gelftest";
        message.fullMessage = "full gelftest\nstuff";
        message.level = 1;
        message.type = 8;
        message.host = "junit-test";
        message.file = "junit-testfile";
        message.line = 9001;

        // Insert the message.
        MongoBridge instance = new MongoBridge();
        instance.insertGelfMessage(message);

        // Fetch the event and compare
        DBCollection coll = instance.getMessagesColl();
        long count = coll.getCount();
        assertTrue(count == 1);

        DBObject res = coll.findOne();
        assertEquals(res.get("message"), "gelftest");
        assertEquals(res.get("full_message"), "full gelftest\nstuff");
        assertEquals(res.get("level"), 1);
        assertEquals(res.get("type"), 8);
        assertEquals(res.get("host"), "junit-test");
        assertEquals(res.get("file"), "junit-testfile");
        assertEquals(res.get("line"), 9001);
    }

    /**
     * Test of distinctHosts method, of class MongoBridge.
     */
    @Test
    public void testDistinctHosts() throws Exception {
        DB db = MongoConnection.getInstance().getDatabase();
        MongoBridge m = new MongoBridge();

        // Insert a message.
        GELFMessage message = new GELFMessage();
        message.shortMessage = "test";
        message.host = "host1";
        m.insertGelfMessage(message);

        // Second message from another host
        message.host = "host2";
        m.insertGelfMessage(message);

        // Distinct the hosts.
        m.distinctHosts();

        DBCollection coll = db.getCollection("hosts");

        List<String> hosts = coll.distinct("host");
        assertTrue(hosts.size() == 2);
    }

}