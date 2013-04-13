/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.graylog2;

import org.graylog2.plugin.Tools;
import java.util.HashMap;
import java.util.Map;
import org.bson.types.ObjectId;
import org.junit.Test;

import com.google.common.collect.Maps;

import static org.junit.Assert.*;

/**
 * @author XING\lennart.koopmann
 */
public class MessageCounterTest {

    private MessageCounterImpl counter = new MessageCounterImpl();

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

        Map<String, Integer> expected = Maps.newHashMap();
        expected.put(stream1.toString(), 1);
        expected.put(stream2.toString(), 5);
        expected.put(stream3.toString(), 2);

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

        Map<String, Integer> expected = Maps.newHashMap();
        expected.put(Tools.encodeBase64(host1), 5);
        expected.put(Tools.encodeBase64(host2), 1);
        expected.put(Tools.encodeBase64(host3), 3);

        counter.countUpStream(new ObjectId(), 5); // Add a stream count for complexity.
        counter.countUpSource(host1, 4);
        counter.countUpSource(host1, 1);
        counter.incrementSource(host2);
        counter.countUpSource(host3, 3);

        assertEquals(expected, counter.getSourceCounts());
    }

    @Test
    public void testResetAllCounts() {
        counter.countUpTotal(100);
        counter.countUpSource("foo.example.org", 9001);
        counter.countUpStream(new ObjectId(), 5);

        assertEquals(100, counter.getTotalCount()); // Just to make sure.

        counter.resetAllCounts();

        assertEquals(0, counter.getTotalCount());
        assertEquals(0, counter.getSourceCounts().size());
        assertEquals(0, counter.getStreamCounts().size());
    }

    @Test
    public void testResetHostCounts() {
        counter.countUpSource("lolwat", 200);
        counter.resetSourceCounts();
        assertEquals(new HashMap<String, Integer>(), counter.getSourceCounts());
    }

    @Test
    public void testResetStreamCounts() {
        counter.countUpStream(new ObjectId(), 100);
        counter.resetStreamCounts();
        assertEquals(new HashMap<ObjectId, Integer>(), counter.getStreamCounts());    }

    @Test
    public void testResetTotal() {
        counter.countUpTotal(1000);
        assertEquals(1000, counter.getTotalCount());
        counter.resetTotal();
        assertEquals(0, counter.getTotalCount());
    }

    @Test
    public void testIncrementTotal() {
        counter.countUpTotal(10);
        counter.incrementTotal();
        assertEquals(11, counter.getTotalCount());
    }

    @Test
    public void testCountUpTotal() {
        counter.countUpTotal(500);
        counter.countUpTotal(50);
        assertEquals(550, counter.getTotalCount());
    }

    @Test
    public void testIncrementStream() {
        ObjectId streamId = new ObjectId();
        counter.countUpStream(streamId, 100);
        counter.incrementStream(streamId);

        int res = counter.getStreamCounts().get(streamId.toString());
        assertEquals(101, res);
    }

    @Test
    public void testCountUpStream() {
        ObjectId streamId = new ObjectId();
        counter.countUpStream(streamId, 100);
        counter.countUpStream(streamId, 150);

        int res = counter.getStreamCounts().get(streamId.toString());
        assertEquals(250, res);
    }

    @Test
    public void testIncrementHost() {
        String hostname = "foobar";
        counter.countUpSource(hostname, 10);
        counter.incrementSource(hostname);

        int res = counter.getSourceCounts().get(Tools.encodeBase64(hostname));
        assertEquals(11, res);
    }

    @Test
    public void testCountUpHost() {
        String hostname = "foo.example.org";
        counter.countUpSource(hostname, 25);
        counter.countUpSource(hostname, 40);

        int res = counter.getSourceCounts().get(Tools.encodeBase64(hostname));
        assertEquals(65, res);
    }

}