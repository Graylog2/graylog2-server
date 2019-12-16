package org.graylog2.shared.messageq;

import java.util.function.Consumer;

public interface MessageQueueReader extends MessageQueue {
    interface Factory<TYPE extends MessageQueueReader> {
        TYPE create(String name);
    }

    MessageQueue.Envelope read(long entries) throws MessageQueueException;

    void subscribe(Consumer<MessageQueue.Envelope> consumer) throws MessageQueueException;
}
