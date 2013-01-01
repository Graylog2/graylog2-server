/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.graylog2;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.bson.types.ObjectId;
import org.graylog2.plugin.Counter;
import org.graylog2.plugin.Tools;
import org.junit.Test;

import com.google.common.collect.Maps;

/**
 * @author XING\lennart.koopmann
 */
public class MessageCounterTest {

    private MessageCounterImpl counter = new MessageCounterImpl();

    @Test
    public void testGetTotalCount() {
        counter.countUpTotal(5);
        counter.incrementTotal();
        assertEquals(CounterFactory.newCounter(6), counter.getTotalCount());
    }

    @Test
    public void testGetStreamCounts() {
        ObjectId stream1 = new ObjectId();
        ObjectId stream2 = new ObjectId();
        ObjectId stream3 = new ObjectId();

        Map<String, Counter> expected = Maps.newHashMap();
        expected.put(stream1.toString(), CounterFactory.newCounter(1));
        expected.put(stream2.toString(), CounterFactory.newCounter(5));
        expected.put(stream3.toString(), CounterFactory.newCounter(2));

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

        Map<String, Counter> expected = Maps.newHashMap();
        expected.put(Tools.encodeBase64(host1), CounterFactory.newCounter(5));
        expected.put(Tools.encodeBase64(host2), CounterFactory.newCounter(1));
        expected.put(Tools.encodeBase64(host3), CounterFactory.newCounter(3));

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

        assertEquals(CounterFactory.newCounter(100), counter.getTotalCount()); // Just to make sure.

        counter.resetAllCounts();

        assertEquals(CounterFactory.newCounter(0), counter.getTotalCount());
        assertEquals(0, counter.getHostCounts().size());
        assertEquals(0, counter.getStreamCounts().size());
    }

    @Test
    public void testResetHostCounts() {
        counter.countUpHost("lolwat", 200);
        counter.resetHostCounts();
        assertEquals(new HashMap<String, Integer>(), counter.getHostCounts());
    }

    @Test
    public void testResetStreamCounts() {
        counter.countUpStream(new ObjectId(), 100);
        counter.resetStreamCounts();
        assertEquals(new HashMap<ObjectId, Integer>(), counter.getStreamCounts());
    }

    @Test
    public void testResetTotal() {
        counter.countUpTotal(1000);
        assertEquals(CounterFactory.newCounter(1000), counter.getTotalCount());
        counter.resetTotal();
        assertEquals(CounterFactory.newCounter(0), counter.getTotalCount());
    }

    @Test
    public void testIncrementTotal() {
        counter.countUpTotal(10);
        counter.incrementTotal();
        assertEquals(CounterFactory.newCounter(11), counter.getTotalCount());
    }

    @Test
    public void testCountUpTotal() {
        counter.countUpTotal(500);
        counter.countUpTotal(50);
        assertEquals(CounterFactory.newCounter(550), counter.getTotalCount());
    }

    @Test
    public void testIncrementStream() {
        ObjectId streamId = new ObjectId();
        counter.countUpStream(streamId, 100);
        counter.incrementStream(streamId);

        Counter res = counter.getStreamCounts().get(streamId.toString());
        assertEquals(CounterFactory.newCounter(101), res);
    }

    @Test
    public void testCountUpStream() {
        ObjectId streamId = new ObjectId();
        counter.countUpStream(streamId, 100);
        counter.countUpStream(streamId, 150);

        Counter res = counter.getStreamCounts().get(streamId.toString());
        assertEquals(CounterFactory.newCounter(250), res);
    }

    @Test
    public void testIncrementHost() {
        String hostname = "foobar";
        counter.countUpHost(hostname, 10);
        counter.incrementHost(hostname);

        Counter res = counter.getHostCounts().get(Tools.encodeBase64(hostname));
        assertEquals(CounterFactory.newCounter(11), res);
    }

    @Test
    public void testCountUpHost() {
        String hostname = "foo.example.org";
        counter.countUpHost(hostname, 25);
        counter.countUpHost(hostname, 40);

        Counter res = counter.getHostCounts().get(Tools.encodeBase64(hostname));
        assertEquals(CounterFactory.newCounter(65), res);
    }

}