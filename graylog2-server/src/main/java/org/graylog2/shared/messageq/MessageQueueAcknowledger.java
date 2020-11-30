package org.graylog2.shared.messageq;

import java.util.List;

public interface MessageQueueAcknowledger {

    void acknowledge(Object messageId) throws MessageQueueException;

    void acknowledge(List<Object> messageIds) throws MessageQueueException;
}
