package org.graylog2.shared.messageq;

import com.google.inject.Scopes;
import com.google.inject.name.Names;
import org.graylog2.plugin.PluginModule;
import org.graylog2.shared.messageq.pulsar.PulsarMessageQueueReader;
import org.graylog2.shared.messageq.pulsar.PulsarMessageQueueWriter;

public class MessageQueueModule extends PluginModule {
    public static final String INPUT_MESSAGE_QUEUE_TYPE = "input-message-queue-type";

    @Override
    protected void configure() {
        bind(String.class).annotatedWith(Names.named(INPUT_MESSAGE_QUEUE_TYPE)).toInstance("pulsar");

        serviceBinder().addBinding().to(PulsarMessageQueueWriter.class).in(Scopes.SINGLETON);
        serviceBinder().addBinding().to(PulsarMessageQueueReader.class).in(Scopes.SINGLETON);
        bind(MessageQueueReader.class).to(PulsarMessageQueueReader.class).in(Scopes.SINGLETON);
        bind(MessageQueueWriter.class).to(PulsarMessageQueueWriter.class).in(Scopes.SINGLETON);
    }

}
