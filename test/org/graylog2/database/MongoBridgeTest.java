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
import org.productivity.java.syslog4j.server.SyslogServerEventIF;
import org.productivity.java.syslog4j.server.impl.event.SyslogServerEvent;

/**
 *
 * @author lennart
 */
public class MongoBridgeTest {

    public MongoBridgeTest() {
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
        System.out.println("A");
        // Build an event.
        SyslogServerEventIF event = new SyslogServerEvent(null, 0, null);
        event.setMessage("testmessage");
        event.setHost("testhost");
        event.setFacility(4);
        event.setLevel(5);
        System.out.println("B");

        // Insert the event.
        MongoBridge instance = new MongoBridge();
        instance.insert(event);
    }

    /**
     * Test of insertGelfMessage method, of class MongoBridge.
     */
    @Test
    public void testInsertGelfMessage() throws Exception {
        System.out.println("insertGelfMessage");
        GELFMessage message = null;
        MongoBridge instance = new MongoBridge();
        instance.insertGelfMessage(message);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
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