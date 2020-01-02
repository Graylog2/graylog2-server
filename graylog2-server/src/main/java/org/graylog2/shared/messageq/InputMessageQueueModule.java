package org.graylog2.shared.messageq;

import com.google.inject.Scopes;
import org.graylog2.shared.messageq.pulsar.PulsarMessageQueueReader;
import org.graylog2.shared.messageq.pulsar.PulsarMessageQueueWriter;

public class InputMessageQueueModule extends MessageQueueModule {

    @Override
    protected void configure() {
        serviceBinder().addBinding().to(PulsarMessageQueueWriter.class).in(Scopes.SINGLETON);
        serviceBinder().addBinding().to(PulsarMessageQueueReader.class).in(Scopes.SINGLETON);
    }
}
