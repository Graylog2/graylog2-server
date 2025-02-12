package org.graylog2.indexer.messages;

import com.github.joschi.jadconfig.util.Duration;
import com.github.rholder.retry.WaitStrategies;
import com.github.rholder.retry.WaitStrategy;
import com.google.common.annotations.VisibleForTesting;

public class RetryWait {
    static final Duration MAX_WAIT_TIME = Duration.seconds(30L);
    @VisibleForTesting
    final WaitStrategy waitStrategy;

    public RetryWait(int retrySecondsMultiplier) {
        this.waitStrategy = WaitStrategies.exponentialWait(retrySecondsMultiplier, MAX_WAIT_TIME.getQuantity(), MAX_WAIT_TIME.getUnit());
    }

    public void waitBeforeRetrying(long attempt) {
        try {
            final long sleepTime = waitStrategy.computeSleepTime(new GenericRetryAttempt(attempt));
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
