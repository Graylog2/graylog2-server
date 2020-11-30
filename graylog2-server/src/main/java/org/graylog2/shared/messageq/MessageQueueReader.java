package org.graylog2.shared.messageq;

import java.util.List;

public interface MessageQueueReader extends MessageQueue {

    List<Entry> read(long entries) throws MessageQueueException;

    void commit(Object messageId) throws MessageQueueException;
}
