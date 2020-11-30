package org.graylog2.shared.messageq;

import com.google.inject.Scopes;
import org.graylog2.plugin.PluginModule;
import org.graylog2.shared.messageq.kafka.KafkaMessageQueueAcknowledger;
import org.graylog2.shared.messageq.kafka.KafkaMessageQueueReader;
import org.graylog2.shared.messageq.kafka.KafkaMessageQueueWriter;

public class MessageQueueModule extends PluginModule {

    @Override
    protected void configure() {
        // TODO unify with JournalReaderModule

        // Journal with Pulsar
//        serviceBinder().addBinding().to(PulsarMessageQueueWriter.class).in(Scopes.SINGLETON);
//        serviceBinder().addBinding().to(PulsarMessageQueueReader.class).in(Scopes.SINGLETON);
//        bind(MessageQueueReader.class).to(PulsarMessageQueueReader.class).in(Scopes.SINGLETON);
//        bind(MessageQueueWriter.class).to(PulsarMessageQueueWriter.class).in(Scopes.SINGLETON);
//        bind(MessageQueueAcknowledger.class).to(PulsarMessageQueueAcknowledger.class).in(Scopes.SINGLETON);

        // Journal with Kafka
        bind(MessageQueueReader.class).to(KafkaMessageQueueReader.class).in(Scopes.SINGLETON);
        bind(MessageQueueWriter.class).to(KafkaMessageQueueWriter.class).in(Scopes.SINGLETON);
        bind(MessageQueueAcknowledger.class).to(KafkaMessageQueueAcknowledger.class).in(Scopes.SINGLETON);

        // TODO no Journal
    }

}
