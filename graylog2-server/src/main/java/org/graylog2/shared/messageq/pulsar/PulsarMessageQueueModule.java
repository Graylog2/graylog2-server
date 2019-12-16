package org.graylog2.shared.messageq.pulsar;

import org.graylog2.shared.messageq.MessageQueueModule;

public class PulsarMessageQueueModule extends MessageQueueModule {
    @Override
    protected void configure() {
        addMessageQueue("pulsar",
                PulsarMessageQueueWriter.class,
                PulsarMessageQueueWriter.Factory.class,
                PulsarMessageQueueReader.class,
                PulsarMessageQueueReader.Factory.class);
    }
}
