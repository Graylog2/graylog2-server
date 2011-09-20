/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.graylog2.messagehandlers.common;

import java.util.HashMap;
import java.util.Map;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author XING\lennart.koopmann
 */
public class MessageCounterTest {

    private MessageCounter counter = MessageCounter.getInstance();

    @Before
    public void setUp() {
        counter.resetAllCounts();
    }

    @Test
    public void testGetInstance() {
        assertTrue(MessageCounter.getInstance() instanceof MessageCounter);
    }

    @Test
    public void testGetTotalCount() {
        counter.countUpTotal(5);
        counter.incrementTotal();
        assertEquals(6, counter.getTotalCount());
    }

    @Test
    public void testGetStreamCounts() {
        ObjectId stream1 = new ObjectId();
        ObjectId stream2 = new ObjectId();
        ObjectId stream3 = new ObjectId();

        Map expected = new HashMap<ObjectId, Integer>();
        expected.put(stream1, 1);
        expected.put(stream2, 5);
        expected.put(stream3, 2);

        counter.countUpStream(stream1, 1);
        counter.countUpStream(stream2, 3);
        counter.countUpStream(stream2, 2);
        counter.countUpStream(stream3, 1);
        counter.incrementStream(stream3);

        assertEquals(expected, counter.getStreamCounts());
    }

    @Test
    public void testGetHostCounts() {
        String host1 = "example.org";
        String host2 = "foo.example.org";
        String host3 = "example.com";

        Map expected = new HashMap<String, Integer>();
        expected.put(host1, 5);
        expected.put(host2, 1);
        expected.put(host3, 3);

        counter.countUpStream(new ObjectId(), 5); // Add a stream count for complexity.
        counter.countUpHost(host1, 4);
        counter.countUpHost(host1, 1);
        counter.incrementHost(host2);
        counter.countUpHost(host3, 3);

        assertEquals(expected, counter.getHostCounts());
    }

    @Test
    public void testResetAllCounts() {
        counter.countUpTotal(100);
        counter.countUpHost("foo.example.org", 9001);
        counter.countUpStream(new ObjectId(), 5);

        assertEquals(100, counter.getTotalCount()); // Just to make sure.

        counter.resetAllCounts();

        assertEquals(0, counter.getTotalCount());
        assertEquals(0, counter.getHostCounts().size());
        assertEquals(0, counter.getStreamCounts().size());
    }

    @Test
    public void testResetHostCounts() {
        fail("The test case is a prototype.");
    }

    @Test
    public void testResetStreamCounts() {
        fail("The test case is a prototype.");
    }

    @Test
    public void testResetTotal() {
        fail("The test case is a prototype.");
    }

    @Test
    public void testIncrementTotal() {
        fail("The test case is a prototype.");
    }

    @Test
    public void testCountUpTotal() {
        fail("The test case is a prototype.");
    }

    @Test
    public void testIncrementStream() {
        fail("The test case is a prototype.");
    }

    @Test
    public void testCountUpStream() {
        fail("The test case is a prototype.");
    }

    @Test
    public void testIncrementHost() {
        fail("The test case is a prototype.");
    }

    @Test
    public void testCountUpHost() {
        fail("The test case is a prototype.");
    }

}