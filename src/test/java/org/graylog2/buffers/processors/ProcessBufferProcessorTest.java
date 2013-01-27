/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.graylog2.buffers.processors;

import org.graylog2.Configuration;
import org.graylog2.GraylogServerStub;
import org.graylog2.buffers.ProcessBuffer;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;
import org.graylog2.plugin.logmessage.LogMessage;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.BitSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author lennart.koopmann
 */
public class ProcessBufferProcessorTest {

    public ProcessBufferProcessorTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Test
    public void testOnEvent() throws Exception {
    }

    @Test
    public void testBatching() throws Exception {
        final QueueBuffer queueBuffer = new QueueBuffer();
        GraylogServerStub serverStub = new GraylogServerStub() {
            @Override
            public Buffer getOutputBuffer() {
                return queueBuffer;
            }
        };
        Configuration configStub = new Configuration();
        serverStub.setConfigurationStub(configStub);
        int bufferSize = configStub.getRingSize();

        ProcessBuffer buffer = new ProcessBuffer(serverStub);
        buffer.initialize();

        AtomicInteger counter = new AtomicInteger();

        // Empty buffer
        Assert.assertTrue(buffer.hasCapacity());
        Assert.assertTrue(buffer.hasCapacity(bufferSize));
        Assert.assertFalse(buffer.hasCapacity(bufferSize + 1));

        // Overflow (use separate counter as these messages will be thrown away)
        try {
            buffer.insert(messages(new AtomicInteger(bufferSize), bufferSize + 1));
            Assert.fail();
        } catch (IllegalStateException e) { }

        // Add one message
        buffer.insert(message(counter));

        Assert.assertTrue(buffer.hasCapacity());
        Assert.assertTrue(buffer.hasCapacity(bufferSize - 1));
        Assert.assertFalse(buffer.hasCapacity(bufferSize));
        Assert.assertFalse(buffer.hasCapacity(bufferSize + 1));

        // Top up to size / 2  messages
        buffer.insert(messages(counter, (bufferSize / 2) - counter.get()));

        Assert.assertTrue(buffer.hasCapacity());
        Assert.assertTrue(buffer.hasCapacity(bufferSize / 2));
        Assert.assertFalse(buffer.hasCapacity((bufferSize / 2) + 1));
        Assert.assertFalse(buffer.hasCapacity(bufferSize));
        Assert.assertFalse(buffer.hasCapacity(bufferSize + 1));

        // Overflow (use separate counter as these messages will be thrown away)
        try {
            buffer.insert(messages(new AtomicInteger(bufferSize * 2), (bufferSize / 2) + 1));
            Assert.fail();
        } catch (BufferOutOfCapacityException e) { }

        Assert.assertTrue(buffer.hasCapacity());
        Assert.assertTrue(buffer.hasCapacity(bufferSize / 2));
        Assert.assertFalse(buffer.hasCapacity((bufferSize / 2) + 1));
        Assert.assertFalse(buffer.hasCapacity(bufferSize));
        Assert.assertFalse(buffer.hasCapacity(bufferSize + 1));

        // Fill
        buffer.insert(messages(counter, (bufferSize / 2)));

        Assert.assertFalse(buffer.hasCapacity());
        Assert.assertFalse(buffer.hasCapacity(2));

        // Drain and verify all messages delivered (multiple processor threads so cannot guarantee this will be in order)
        queueBuffer.insertLatch.countDown();

        BitSet seenMessages = new BitSet();

        for (int i = 0; i < bufferSize; i++) {
            int messageId = drainMessage(queueBuffer);
            if (seenMessages.get(messageId)) Assert.fail("Received message " + messageId + " twice");
            seenMessages.set(messageId);
        }

        int firstClearBit = seenMessages.nextClearBit(0);
        Assert.assertEquals("Missing message " + firstClearBit, bufferSize, firstClearBit);
    }

    private int drainMessage(QueueBuffer queueBuffer) throws InterruptedException {
        LogMessage message = queueBuffer.messages.poll(10, TimeUnit.SECONDS);
        Assert.assertNotNull(message);
        return Integer.parseInt(message.getShortMessage());
    }

    private LogMessage message(AtomicInteger i) {
        LogMessage message = new LogMessage();
        message.setShortMessage(Integer.toString(i.getAndIncrement()));
        return message;
    }

    private LogMessage[] messages(AtomicInteger i, int count) {
        LogMessage[] messages = new LogMessage[count];
        for (int j = 0; j < messages.length; j++) {
            messages[j] = message(i);
        }
        return messages;
    }

    private static class QueueBuffer implements Buffer {
        final LinkedBlockingQueue<LogMessage> messages = new LinkedBlockingQueue<LogMessage>();
        final CountDownLatch insertLatch = new CountDownLatch(1);

        @Override
        public void insert(LogMessage logMessage) throws BufferOutOfCapacityException {
            messages.add(logMessage);
            try {
                insertLatch.await();
            } catch (InterruptedException ignored) { }
        }

        @Override
        public boolean hasCapacity() {
            return true;
        }
    }
}