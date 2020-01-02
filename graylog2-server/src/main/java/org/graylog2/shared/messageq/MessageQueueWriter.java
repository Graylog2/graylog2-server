package org.graylog2.shared.messageq;

import java.util.List;

public interface MessageQueueWriter extends MessageQueue {

    void write(List<MessageQueue.Entry> entries) throws MessageQueueException;
}
