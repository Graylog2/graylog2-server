package org.graylog2.shared.messageq;

import com.google.inject.assistedinject.Assisted;

import java.util.List;

public interface MessageQueueWriter extends MessageQueue {
    interface Factory<TYPE extends MessageQueueWriter> {
        TYPE create(@Assisted("name") String name);
    }

    void write(List<MessageQueue.Entry> entries) throws MessageQueueException;
}
