package org.graylog.testing.utils;

import org.glassfish.jersey.internal.util.Producer;

import static org.junit.Assert.fail;

public final class WaitUtils {

    private WaitUtils() {}

    public static  void waitFor(Producer<Boolean> predicate, String timeoutErrorMessage) {
        int timeOutMs = 5000;
        int msPassed = 0;
        int waitMs = 500;
        while (msPassed <= timeOutMs) {
            if (predicate.call()) {
                return;
            }
            msPassed += waitMs;
            wait(waitMs);
        }
        fail(timeoutErrorMessage);
    }

    private static  void wait(int waitMs) {
        try {
            Thread.sleep(waitMs);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
