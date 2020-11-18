/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.plugin.inputs.transports;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog2.plugin.ThrottleState;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Placeholder class for implementing logic to throttle certain transports which support back pressure.
 * The built in transports that support this by reading less are the Kafka and AMQP transports.
 * <br/>
 * Empty for now, since the process buffer provides natural throttling for now, but once that is async we need
 * to supply back pressure in some other way.
 */
public abstract class ThrottleableTransport implements Transport {
    private static final Logger log = LoggerFactory.getLogger(ThrottleableTransport.class);
    public static final String CK_THROTTLING_ALLOWED = "throttling_allowed";
    private final boolean throttlingAllowed;
    private final AtomicBoolean currentlyThrottled = new AtomicBoolean(false);
    private final EventBus eventBus;
    private volatile CountDownLatch blockLatch = null;
    private long lastUncommitted;

    public static class Config implements Transport.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest request = new ConfigurationRequest();

            request.addField(new BooleanField(
                    CK_THROTTLING_ALLOWED,
                    "Allow throttling this input.",
                    false,
                    "If enabled, no new messages will be read from this input until Graylog catches up with its message load. " +
                    "This is typically useful for inputs reading from files or message queue systems like AMQP or Kafka. " +
                    "If you regularly poll an external system, e.g. via HTTP, you normally want to leave this disabled."

            ));
            return request;
        }
    }

    public ThrottleableTransport(EventBus eventBus, Configuration configuration) {
        this.eventBus = eventBus;
        this.throttlingAllowed = configuration.getBoolean(CK_THROTTLING_ALLOWED);
    }

    @Override
    public void launch(MessageInput input) throws MisfireException {
        // Call this before registering on the event bus. There might be stuff in doLaunch() that needs to run first.
        doLaunch(input);

        // only listen for updates if we are allowed to be throttled at all
        if (throttlingAllowed) {
            eventBus.register(this);
        }
    }

    /**
     * Performs the same purpose as {@link #launch(org.graylog2.plugin.inputs.MessageInput)} but guarantees that the superclass'
     * actions are performed.
     * @param input
     * @throws MisfireException
     */
    protected abstract void doLaunch(MessageInput input) throws MisfireException;

    @Override
    public void stop() {
        // always unblock the transport when shutting down to avoid deadlock
        if (currentlyThrottled.get()) {
            blockLatch.countDown();
        }
        // Call this before unregistering from the eventbus. There might be another call to unregister in doStop()
        // which is not protected by try/catch.
        doStop();

        if (throttlingAllowed) {
            try {
                eventBus.unregister(this);
            } catch (IllegalArgumentException ignored) {
                // Ignored. This will be thrown if the object has been unregistered before.
            }
        }
    }

    /**
     * Performs the same purpose as {@link #stop()} but guarantees that the superclass'  actions are performed.
     */
    protected abstract void doStop();

    /**
     * Only executed if the Allow Throttling checkbox is set in the input's configuration.
     * @param throttleState current processing system state
     */
    @Subscribe
    public void updateThrottleState(ThrottleState throttleState) {
        // Only run if throttling is enabled.
        if (!throttlingAllowed) {
            return;
        }
        // check if we are throttled
        final boolean throttled = determineIfThrottled(throttleState);
        if (currentlyThrottled.get()) {
            // no need to unblock
            if (throttled) {
                return;
            }
            // sanity check
            if (blockLatch == null) {
                log.error("Expected to see a transport throttle latch, but it is missing. This is a bug, continuing anyway.");
                return;
            }
            currentlyThrottled.set(false);
            handleChangedThrottledState(false);
            blockLatch.countDown();
        } else if (throttled) {
            currentlyThrottled.set(true);
            handleChangedThrottledState(true);
            blockLatch = new CountDownLatch(1);
        }
    }

    /**
     * Transports can override this to be notified when the throttled state changes. Only called when throttled state changes.
     *
     * @param isThrottled the current throttled state.
     */
    public void handleChangedThrottledState(boolean isThrottled) {

    }

    public boolean isThrottled() {
        return throttlingAllowed && currentlyThrottled.get();
    }

    /**
     * This method implements the default algorithm for determining whether a transport will be throttled or not.
     * <p>
     * Override this method in your subclass if you need to customize the decision.
     * </p>
     * <p>
     * If the transport was started without the <code>throttling_allowed</code> flag enabled, this method will <b>not</b> be called!
     * </p>
     * @param state the current state of the processing system
     * @return true if transport should be throttled, false if not.
     */
    protected boolean determineIfThrottled(ThrottleState state) {
        final long prevUncommitted = lastUncommitted;
        lastUncommitted = state.uncommittedJournalEntries;

        final String transportName = this.getClass().getSimpleName();
        log.debug("Checking if transport {} should be throttled {}", transportName, state);
        if (state.uncommittedJournalEntries == 0) {
            // journal is completely empty, let's read some stuff
            log.debug("[{}] [unthrottled] journal empty", transportName);
            return false;
        }
        if (state.uncommittedJournalEntries > 100_000) {
            log.debug("[{}] [throttled] number of unread journal entries is larger than 100.000 entries: {}", transportName, state.uncommittedJournalEntries);
            return true;
        }
        if (state.uncommittedJournalEntries - prevUncommitted > 20_000) {
            // journal is growing, don't read more
            log.debug("[{}] [throttled] number of unread journal entries is growing by more than 20.000 entries: {}", transportName, state.uncommittedJournalEntries - prevUncommitted);
            return true;
        }
        if (state.processBufferCapacity == 0) {
            log.debug("[{}] [throttled] no capacity in process buffer", transportName);
            return true;
        }
        if (state.appendEventsPerSec == 0 && state.readEventsPerSec == 0 && state.processBufferCapacity > 0) {
            // no one writes anything, it's ok to get more events
            log.debug("[{}] [unthrottled] no incoming messages and nothing read from journal even if we could", transportName);
            return false;
        }
        if ((state.journalSize / (double) state.journalSizeLimit) * 100.0 > 90) {
            // more than 90% of the journal limit is in use, don't read more if possible to avoid throwing away data
            log.debug("[{}] [throttled] journal more than 90% full", transportName);
            return true;
        }
        if ((state.readEventsPerSec / (double) state.appendEventsPerSec) * 100.0 < 50) {
            // read rate is less than 50% of what we write to the journal over the last second, let's try to back off
            log.debug("[{}] [throttled] write rate is more than twice as high than read rate", transportName);
            return true;
        }
        log.debug("[{}] [unthrottled] fall through", transportName);
        return false;
    }

    /**
     * Blocks until the blockLatch is released.
     */
    public void blockUntilUnthrottled() {
        // sanity: if there's no latch, don't try to access it
        if (blockLatch == null) {
            return;
        }
        // purposely allow interrupts as a means to let the caller check if it should exit its run loop
        try {
            blockLatch.await();
        } catch (InterruptedException e) {
            // ignore
        }
    }

    /**
     * Blocks until the blockLatch is released or until the timeout is exceeded.
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit for the {@code timeout} argument.
     * @return        {@code true} if the blockLatch was released before the {@code timeout} elapsed. and
     *                {@code false} if the {@code timeout} was exceeded before the blockLatch was released.
     */
    public boolean blockUntilUnthrottled(long timeout, TimeUnit unit) {
        // sanity: if there's no latch, don't try to access it
        if (blockLatch == null) {
            return false;
        }
        // purposely allow interrupts as a means to let the caller check if it should exit its run loop
        try {
            return blockLatch.await(timeout, unit);
        } catch (InterruptedException e) {
            return false;
        }
    }
}
