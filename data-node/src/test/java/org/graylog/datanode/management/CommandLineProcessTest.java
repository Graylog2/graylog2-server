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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class CommandLineProcessTest {

    @Test
    void testManualStop() throws IOException, ExecutionException, InterruptedException, TimeoutException, RetryException, URISyntaxException {
        final URL bin = getClass().getResource("test-script.sh");
        assert bin != null;
        final Path binPath = Path.of(bin.toURI());

        List<String> stdout = new LinkedList<>();
        List<String> stdErr = new LinkedList<>();

        final ProcessListener listener = new ProcessListener() {
            @Override
            public void onStart() {
            }

            @Override
            public void onStdOut(String line) {
                stdout.add(line);
            }

            @Override
            public void onStdErr(String line) {
                stdErr.add(line);
            }

            @Override
            public void onProcessComplete(int exitValue) {
            }

            @Override
            public void onProcessFailed(ExecuteException e) {
            }
        };
        final CommandLineProcess process = new CommandLineProcess(binPath, Collections.emptyList(), listener);
        process.start();

        waitTillLogsAreAvailable(stdout, 3);
        waitTillLogsAreAvailable(stdErr, 1);

        // if the lines are there, it switches to infinite loop. We'll have to terminate it
        process.stop();

        Assertions.assertThat(stdout)
                .hasSize(3)
                .containsExactlyInAnyOrder("Hello World", "second line", "third line");

        Assertions.assertThat(stdErr)
                .hasSize(1)
                .contains("This message goes to stderr");

    }

    private void waitTillLogsAreAvailable(List<String> logs, int expectedLinesCount) throws ExecutionException, RetryException {
        final Retryer<List<String>> retryer = RetryerBuilder.<List<String>>newBuilder()
                .withWaitStrategy(WaitStrategies.fixedWait(100, TimeUnit.MILLISECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(20))
                .retryIfResult((res) -> res.size() < expectedLinesCount)
                .build();


        retryer.call(() -> logs);
    }

    @Test
    void testExitCode() throws IOException, URISyntaxException, ExecutionException, InterruptedException, TimeoutException {
        final URL bin = getClass().getResource("test-script.sh");
        assert bin != null;
        final Path binPath = Path.of(bin.toURI());

        List<String> stdout = new LinkedList<>();
        List<String> stdErr = new LinkedList<>();

        final CompletableFuture<Integer> exitCodeFuture = new CompletableFuture<>();

        final ProcessListener listener = new ProcessListener() {
            @Override
            public void onStart() {
            }

            @Override
            public void onStdOut(String line) {
            }

            @Override
            public void onStdErr(String line) {
            }

            @Override
            public void onProcessComplete(int exitValue) {
                exitCodeFuture.complete(exitValue);
            }

            @Override
            public void onProcessFailed(ExecuteException e) {
                exitCodeFuture.complete(e.getExitValue());
            }
        };
        final CommandLineProcess process = new CommandLineProcess(binPath, List.of("143"), listener);
        process.start();

        final Integer exitCode = exitCodeFuture.get(10, TimeUnit.SECONDS);

        Assertions.assertThat(exitCode).isEqualTo(143);
    }
}
