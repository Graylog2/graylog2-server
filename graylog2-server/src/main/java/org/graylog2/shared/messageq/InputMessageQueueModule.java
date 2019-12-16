package org.graylog2.shared.messageq;

import com.google.common.util.concurrent.Service;
import com.google.inject.Provides;
import org.graylog2.shared.messageq.annotations.InputMessageQueue;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

public class InputMessageQueueModule extends MessageQueueModule {
    private static final String QUEUE_NAME = "input";

    @Override
    protected void configure() {
        serviceBinder().addBinding().toProvider(InputMessageQueueWriterServiceProvider.class);
        serviceBinder().addBinding().toProvider(InputMessageQueueReaderServiceProvider.class);
    }

    @Provides
    @InputMessageQueue
    public MessageQueueWriter provideWriter(@Named(INPUT_MESSAGE_QUEUE_TYPE) String messageQueueType,
                                            MessageQueueInstanceCache cache) {
        return cache.getWriter(messageQueueType, QUEUE_NAME);
    }

    @Provides
    @InputMessageQueue
    public MessageQueueReader provideReader(@Named(INPUT_MESSAGE_QUEUE_TYPE) String messageQueueType,
                                            MessageQueueInstanceCache cache) {
        return cache.getReader(messageQueueType, QUEUE_NAME);
    }

    private static class InputMessageQueueWriterServiceProvider implements Provider<Service> {
        private final MessageQueueWriter writer;

        @Inject
        public InputMessageQueueWriterServiceProvider(@InputMessageQueue MessageQueueWriter writer) {
            this.writer = writer;
        }

        @Override
        public Service get() {
            return writer;
        }
    }

    private static class InputMessageQueueReaderServiceProvider implements Provider<Service> {
        private final MessageQueueReader reader;

        @Inject
        public InputMessageQueueReaderServiceProvider(@InputMessageQueue MessageQueueReader reader) {
            this.reader = reader;
        }

        @Override
        public Service get() {
            return reader;
        }
    }
}
