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
            "127.0.0.1",
            "graylog2test",
            Integer.valueOf(27017),
            "false",
            null
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
     * Test of insertGelfMessage method, of class MongoBridge.
     */
    @Test
    public void testInsertGelfMessage() throws Exception {
        GELFMessage message = new GELFMessage();
        message.setShortMessage("gelftest");
        message.setFullMessage("full gelftest\nstuff");
        message.setLevel(1);
        message.setHost("junit-test");
        message.setFile("junit-testfile");
        message.setVersion("1.0");
        message.setLine(9001);
        message.addAdditionalData("something", "yepp");

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
        assertEquals(res.get("host"), "junit-test");
        assertEquals(res.get("file"), "junit-testfile");
        assertEquals(res.get("line"), 9001);
        assertEquals(res.get("version"), "1.0");
        assertEquals(res.get("something"), "yepp");
    }

}