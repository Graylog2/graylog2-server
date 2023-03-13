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
package org.graylog.datanode.management;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import org.assertj.core.api.Assertions;
import org.graylog.datanode.process.ProcessState;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

class ProcessWatchdogTest {


    @Test
    void testLifecycle() throws IOException, ExecutionException, RetryException {
        final TestableProcess process = new TestableProcess();
        final ProcessWatchdog watchdog = new ProcessWatchdog(process, 100);
        process.start();
        watchdog.start();

        // both process and watchdog are running now. Let's stop the process and see if the watchdog will restart it
        process.stop();

        // see if the process is starting again
        Assertions.assertThat(isInStartingState(process)).isTrue();

        // repeat
        process.stop();
        Assertions.assertThat(isInStartingState(process)).isTrue();

        process.stop();
        Assertions.assertThat(isInStartingState(process)).isTrue();

        // this is the 4th termination, we give up trying
        process.stop();

        Assertions.assertThat(isWatchdogTerminated(watchdog)).isTrue();
        Assertions.assertThat(process.isInState(ProcessState.TERMINATED)).isTrue();
    }

    private boolean isInStartingState(TestableProcess process) throws ExecutionException, RetryException {
        final Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
                .withWaitStrategy(WaitStrategies.fixedWait(100, TimeUnit.MILLISECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(20))
                .retryIfResult(Boolean.FALSE::equals)
                .build();

        return retryer.call(() -> process.isInState(ProcessState.STARTING));
    }

    private boolean isWatchdogTerminated(ProcessWatchdog watchdog) throws ExecutionException, RetryException {
        final Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
                .withWaitStrategy(WaitStrategies.fixedWait(100, TimeUnit.MILLISECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(5))
                .retryIfResult(Boolean.FALSE::equals)
                .build();

        return retryer.call(watchdog::isStopped);
    }
}
