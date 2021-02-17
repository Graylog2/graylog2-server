package org.graylog2.shared.messageq;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper service to start and stop message queue reader and writer services.
 */
@Singleton
public class MessageQueueInitializer extends AbstractIdleService {
    private static final Logger log = LoggerFactory.getLogger(MessageQueueInitializer.class);

    private final ServiceManager serviceManager;
    private final MessageQueueReader reader;
    private final MessageQueueWriter writer;

    @Inject
    public MessageQueueInitializer(MessageQueueReader reader, MessageQueueWriter writer) {
        this.reader = reader;
        this.writer = writer;
        serviceManager = new ServiceManager(ImmutableSet.of(reader, writer));
    }

    @Override
    protected void startUp() throws Exception {
        log.info("Starting message queue reader \"{}\" and writer \"{}\".",
                reader.getClass().getSimpleName(),
                writer.getClass().getSimpleName());
        serviceManager.startAsync().awaitHealthy();
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("Stopping message queue reader and writer.");
        serviceManager.stopAsync().awaitStopped();
    }
}
