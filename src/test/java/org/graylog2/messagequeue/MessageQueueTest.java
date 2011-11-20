package org.graylog2.messagequeue;

import org.graylog2.messagehandlers.gelf.GELFMessage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * Unit tests for {@link MessageQueue}
 *
 * @author Jochen Schalanda <jochen@schalanda.name>
 */
public class MessageQueueTest {

    private MessageQueue messageQueue;

    @Before
    public void setUp() {

        messageQueue = MessageQueue.getInstance();

        // Try to get the MessageQueue instance into a pristine state
        messageQueue.open();
        messageQueue.readAll();

        Assert.assertEquals(0, messageQueue.getSize());
    }

    @Test
    public void testSingleton() {

        Assert.assertSame(messageQueue, MessageQueue.getInstance());
    }

    @Test
    public void testAdd() throws QueueLimitReachedException, QueueClosedException {

        addGELFMessages(messageQueue, 1);
        Assert.assertEquals(1, messageQueue.getSize());
    }

    @Test
    public void testPollEmptyQueue() throws QueueLimitReachedException, QueueClosedException {

        Assert.assertNull(messageQueue.poll());
    }

    @Test
    public void testPoll() throws QueueLimitReachedException, QueueClosedException {

        GELFMessage gelfMessage = new GELFMessage();

        messageQueue.add(gelfMessage);

        Assert.assertEquals(1, messageQueue.getSize());
        Assert.assertEquals(gelfMessage, messageQueue.poll());
    }

    @Test
    public void testGetSize() throws QueueLimitReachedException, QueueClosedException {

        Assert.assertEquals(0, messageQueue.getSize());

        addGELFMessages(messageQueue, 1);

        Assert.assertEquals(1, messageQueue.getSize());
        messageQueue.poll();
        Assert.assertEquals(0, messageQueue.getSize());

    }

    @Test
    public void testSetMaximumSize() throws QueueLimitReachedException, QueueClosedException {

        messageQueue.setMaximumSize(2);
        addGELFMessages(messageQueue, 2);

        try {
            addGELFMessages(messageQueue, 1);
            fail("Expected QueueLimitReachedException");
        } catch (QueueLimitReachedException ex) {
            // Exception expected
        }

        Assert.assertEquals(2, messageQueue.getSize());

        messageQueue.setMaximumSize(0);
        addGELFMessages(messageQueue, 1);

        Assert.assertEquals(3, messageQueue.getSize());
    }

    @Test
    public void testOpen() throws QueueLimitReachedException, QueueClosedException {

        messageQueue.close();

        try {
            addGELFMessages(messageQueue, 1);
            fail("Expected QueueClosedException");
        } catch (QueueClosedException ex) {
            // QueueClosedException expected
        }

        messageQueue.open();

        addGELFMessages(messageQueue, 1);
    }

    @Test(expected = QueueClosedException.class)
    public void testClose() throws QueueLimitReachedException, QueueClosedException {

        messageQueue.close();
        addGELFMessages(messageQueue, 1);
    }

    @Test
    public void testReadBatch() throws QueueLimitReachedException, QueueClosedException {

        addGELFMessages(messageQueue, 5);

        Assert.assertEquals(2, messageQueue.readBatch(2).size());
        Assert.assertEquals(3, messageQueue.getSize());
        Assert.assertEquals(3, messageQueue.readBatch(3).size());
        Assert.assertEquals(0, messageQueue.getSize());
        Assert.assertEquals(0, messageQueue.readBatch(100).size());
    }

    @Test
    public void testReadAll() throws QueueLimitReachedException, QueueClosedException {

        addGELFMessages(messageQueue, 5);

        Assert.assertEquals(5, messageQueue.readAll().size());
        Assert.assertEquals(0, messageQueue.getSize());
    }

    private void addGELFMessages(MessageQueue messageQueue, int numMessages) throws QueueLimitReachedException, QueueClosedException {

        for (int i = 0; i < numMessages; i++) {
            messageQueue.add(new GELFMessage());
        }
    }
}
