package org.graylog2.buffers.processors;

import org.graylog2.Configuration;
import org.graylog2.GraylogServerStub;
import org.graylog2.buffers.ProcessBuffer;
import org.graylog2.inputs.raw.RawInputBase;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;
import org.graylog2.plugin.buffers.ProcessingDisabledException;
import org.graylog2.plugin.inputs.MessageInput;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import java.util.BitSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author James Furness
 */
public class ProcessBufferProcessorTest {
    private final static MessageInput DUMMY_INPUT = new RawInputBase();

    public ProcessBufferProcessorTest() {
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

        ProcessBuffer buffer = new ProcessBuffer(serverStub, null);
        buffer.initialize();

        AtomicInteger counter = new AtomicInteger();

        // Empty buffer
        Assert.assertTrue(buffer.hasCapacity());
        Assert.assertTrue(buffer.hasCapacity(bufferSize));
        Assert.assertFalse(buffer.hasCapacity(bufferSize + 1));

        // Overflow (use separate counter as these messages will be thrown away)
        try {
            buffer.insertFailFast(messages(new AtomicInteger(bufferSize), bufferSize + 1), DUMMY_INPUT);
            Assert.fail();
        } catch (IllegalStateException e) {
        }

        // Add one message
        buffer.insertFailFast(message(counter), DUMMY_INPUT);

        Assert.assertTrue(buffer.hasCapacity());
        Assert.assertTrue(buffer.hasCapacity(bufferSize - 1));
        Assert.assertFalse(buffer.hasCapacity(bufferSize));
        Assert.assertFalse(buffer.hasCapacity(bufferSize + 1));

        // Top up to size / 2  messages
        buffer.insertFailFast(messages(counter, (bufferSize / 2) - counter.get()), DUMMY_INPUT);

        Assert.assertTrue(buffer.hasCapacity());
        Assert.assertTrue(buffer.hasCapacity(bufferSize / 2));
        Assert.assertFalse(buffer.hasCapacity((bufferSize / 2) + 1));
        Assert.assertFalse(buffer.hasCapacity(bufferSize));
        Assert.assertFalse(buffer.hasCapacity(bufferSize + 1));

        // Overflow (use separate counter as these messages will be thrown away)
        try {
            buffer.insertFailFast(messages(new AtomicInteger(bufferSize * 2), (bufferSize / 2) + 1), DUMMY_INPUT);
            Assert.fail();
        } catch (BufferOutOfCapacityException e) {
        }

        Assert.assertTrue(buffer.hasCapacity());
        Assert.assertTrue(buffer.hasCapacity(bufferSize / 2));
        Assert.assertFalse(buffer.hasCapacity((bufferSize / 2) + 1));
        Assert.assertFalse(buffer.hasCapacity(bufferSize));
        Assert.assertFalse(buffer.hasCapacity(bufferSize + 1));

        // Fill
        buffer.insertFailFast(messages(counter, (bufferSize / 2)), DUMMY_INPUT);

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
        Message message = queueBuffer.messages.poll(10, TimeUnit.SECONDS);
        Assert.assertNotNull(message);
        return Integer.parseInt(message.getMessage());
    }

    private Message message(AtomicInteger i) {
        return new Message(Integer.toString(i.getAndIncrement()), "test", new DateTime(0));
    }

    private Message[] messages(AtomicInteger i, int count) {
        Message[] messages = new Message[count];
        for (int j = 0; j < messages.length; j++) {
            messages[j] = message(i);
        }
        return messages;
    }

    private static class QueueBuffer extends Buffer {
        final LinkedBlockingQueue<Message> messages = new LinkedBlockingQueue<Message>();
        final CountDownLatch insertLatch = new CountDownLatch(1);

        @Override
        public void insertFailFast(Message message, MessageInput sourceInput) throws BufferOutOfCapacityException, ProcessingDisabledException {
            messages.add(message);
            try {
                insertLatch.await();
            } catch (InterruptedException ignored) {
            }
        }

        @Override
        public void insertCached(Message message, MessageInput sourceInput) {
            messages.add(message);
            try {
                insertLatch.await();
            } catch (InterruptedException ignored) {
            }
        }

        @Override
        public boolean hasCapacity() {
            return true;
        }
    }
}