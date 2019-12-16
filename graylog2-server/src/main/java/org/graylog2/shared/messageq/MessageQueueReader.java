package org.graylog2.shared.messageq;

import java.util.List;
import java.util.function.Consumer;

public interface MessageQueueReader extends MessageQueue {
    interface Factory<TYPE extends MessageQueueReader> {
        TYPE create(String name);
    }

    List<MessageQueue.Entry> read(long entries) throws MessageQueueException;

    void subscribe(Consumer<MessageQueue.Envelope> consumer) throws MessageQueueException;
}
