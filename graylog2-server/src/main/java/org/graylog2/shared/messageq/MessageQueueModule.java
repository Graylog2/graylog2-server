package org.graylog2.shared.messageq;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import org.graylog2.plugin.PluginModule;
import org.graylog2.shared.messageq.pulsar.PulsarMessageQueueModule;

public class MessageQueueModule extends PluginModule {
    public static final String INPUT_MESSAGE_QUEUE_TYPE = "input-message-queue-type";

    @Override
    protected void configure() {
        bind(String.class).annotatedWith(Names.named(INPUT_MESSAGE_QUEUE_TYPE)).toInstance("pulsar");

        bind(MessageQueueInstanceCache.class).asEagerSingleton();

        install(new PulsarMessageQueueModule());
        install(new InputMessageQueueModule());
    }

    protected void addMessageQueue(String name,
                                   Class<? extends MessageQueueWriter> writerClass,
                                   Class<? extends MessageQueueWriter.Factory> writerFactory,
                                   Class<? extends MessageQueueReader> readerClass,
                                   Class<? extends MessageQueueReader.Factory> readerFactory) {
        messageQueueWriterBinder().addBinding(name).to(writerFactory);
        messageQueueReaderBinder().addBinding(name).to(readerFactory);
        install(new FactoryModuleBuilder().implement(MessageQueueWriter.class, writerClass).build(writerFactory));
        install(new FactoryModuleBuilder().implement(MessageQueueReader.class, readerClass).build(readerFactory));
    }

    private MapBinder<String, MessageQueueWriter.Factory> messageQueueWriterBinder() {
        return MapBinder.newMapBinder(binder(), String.class, MessageQueueWriter.Factory.class);
    }

    private MapBinder<String, MessageQueueReader.Factory> messageQueueReaderBinder() {
        return MapBinder.newMapBinder(binder(), String.class, MessageQueueReader.Factory.class);
    }
}
