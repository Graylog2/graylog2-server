package org.graylog2.shared.messageq;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import org.graylog2.plugin.lifecycles.Lifecycle;

public abstract class AbstractMessageQueueReader extends AbstractExecutionThreadService  implements MessageQueueReader {
    private final EventBus eventBus;
    private volatile boolean shouldBeReading;

    public AbstractMessageQueueReader(EventBus eventBus) {
        this.eventBus = eventBus;
        shouldBeReading = false;
    }

    @Override
    protected void startUp() throws Exception {
        super.startUp();
        eventBus.register(this);
    }

    @Override
    protected void shutDown() throws Exception {
        eventBus.unregister(this);
        super.shutDown();
    }

    @Subscribe
    public void listenForLifecycleChanges(Lifecycle lifecycle) {
        switch (lifecycle) {
            case UNINITIALIZED:
            case STARTING:
            case PAUSED:
            case HALTING:
                shouldBeReading = false;
                break;
            case RUNNING:
            case THROTTLED:
                shouldBeReading = true;
                break;
            case FAILED:
                triggerShutdown();
                break;
            default:
                // don't care, keep processing journal
                break;
        }
    }

    /**
     * Indicates if the reader should read from the message queue or if it should currently pause reading. The
     * returned value is affected by lifecycle changes, e.g. during server startup or when processing has stopped it
     * will be false, during normal operation mode it will be true.
     */
    protected boolean shouldBeReading() {
        return shouldBeReading;
    }
}
