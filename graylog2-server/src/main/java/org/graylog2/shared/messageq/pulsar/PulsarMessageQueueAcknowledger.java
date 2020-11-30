package org.graylog2.shared.messageq.pulsar;

import org.graylog2.shared.messageq.MessageQueueAcknowledger;
import org.graylog2.shared.messageq.MessageQueueException;

import javax.inject.Inject;
import java.util.List;

public class PulsarMessageQueueAcknowledger implements MessageQueueAcknowledger {
    private PulsarMessageQueueReader pulsarMessageQueueReader;

    @Inject
    public PulsarMessageQueueAcknowledger(PulsarMessageQueueReader pulsarMessageQueueReader) {
        this.pulsarMessageQueueReader = pulsarMessageQueueReader;
    }

    @Override
    public void acknowledge(Object messageId) throws MessageQueueException {
        pulsarMessageQueueReader.commit(messageId);
    }

    @Override
    public void acknowledge(List<Object> messageIds) throws MessageQueueException {
        for (Object messageId : messageIds) {
            pulsarMessageQueueReader.commit(messageId);
        }
    }
}
