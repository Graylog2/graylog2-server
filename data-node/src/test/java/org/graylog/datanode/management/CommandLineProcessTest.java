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
import org.apache.commons.exec.ExecuteException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class CommandLineProcessTest {

    @Test
    void testProcessLifecycle() throws IOException, ExecutionException, InterruptedException, TimeoutException, RetryException, URISyntaxException {
        final URL bin = getClass().getResource("test-script.sh");
        assert bin != null;
        final Path binPath = Path.of(bin.toURI());

        final CompletableFuture<Integer> exitValueFuture = new CompletableFuture<>();

        final ProcessListener listener = new ProcessListener() {
            @Override
            public void onStart() {
            }

            @Override
            public void onProcessComplete(int exitValue) {
                exitValueFuture.complete(exitValue);
            }

            @Override
            public void onProcessFailed(ExecuteException e) {
                // If we terminate the process from outside it will lead to an execute exception here. The exit value
                // is available in the exception itself.
                exitValueFuture.complete(e.getExitValue());
            }
        };
        final CommandLineProcess process = new CommandLineProcess(binPath, Collections.emptyList(), 10, listener);
        process.start();

        waitTillLogsAreAvailable(process.getLogs());

        // if the lines are there, it switches to infinite loop. We'll have to terminate it
        process.stop();

        final Integer exitValue = exitValueFuture.get(10, TimeUnit.SECONDS);
        Assertions.assertThat(exitValue).isGreaterThan(0);


        Assertions.assertThat(process.getLogs().stdOut())
                .hasSize(3)
                .containsExactlyInAnyOrder("Hello World", "second line", "third line");

        Assertions.assertThat(process.getLogs().stdErr())
                .hasSize(1)
                .contains("This message goes to stderr");

    }

    private static void waitTillLogsAreAvailable(LogsCache logs) throws ExecutionException, RetryException {
        final Retryer<List<String>> retryer = RetryerBuilder.<List<String>>newBuilder()
                .withWaitStrategy(WaitStrategies.fixedWait(100, TimeUnit.MILLISECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(20))
                .retryIfResult((res) -> res.size() < 3)
                .build();

        // now we are waiting till the process produces 3 lines of stdoutput
        retryer.call(logs::stdOut);
    }
}
