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
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteException;
import org.assertj.core.api.Assertions;
import org.graylog.datanode.ProcessProvidingExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class CommandLineProcessTest {

    private static final Logger LOG = LoggerFactory.getLogger(CommandLineProcessTest.class);
    private Path binPath;

    @BeforeEach
    void setUp() throws URISyntaxException, IOException {
        final URL bin = getClass().getResource("test-script.sh");
        assert bin != null;
        binPath = Path.of(bin.toURI());

        // make sure that the binary has the correct permissions before we try to run it
        Files.setPosixFilePermissions(binPath, PosixFilePermissions.fromString("rwxr-xr-x"));
    }

    @Test
    void testManualStop() throws IOException, ExecutionException, RetryException, URISyntaxException {
        List<String> stdout = new LinkedList<>();
        List<String> stdErr = new LinkedList<>();

        final ProcessListener listener = new ProcessListener() {
            @Override
            public void onStart() {
            }

            @Override
            public void onStdOut(String line) {
                LOG.info("Stdout:" + line);
                stdout.add(line);
            }

            @Override
            public void onStdErr(String line) {
                LOG.info("Stderr:" + line);
                stdErr.add(line);
            }

            @Override
            public void onProcessComplete(int exitValue) {
                LOG.info("On process complete:" + exitValue);
            }

            @Override
            public void onProcessFailed(ExecuteException e) {
                LOG.info("On process failed:", e);
            }
        };
        final TestExecutor testExecutor = new TestExecutor();
        final CommandLineProcess process = new CommandLineProcess(
                binPath,
                Collections.emptyList(),
                listener,
                () -> testExecutor,
                () -> Map.of("USER", "test", "JAVA_HOME", "/path/to/jre")
        );
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

        Assertions.assertThat(testExecutor.getEnvironment())
                .doesNotContainKey("JAVA_HOME");
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
    void testExitCode() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        final CompletableFuture<Integer> exitCodeFuture = new CompletableFuture<>();

        final ProcessListener listener = new ProcessListener() {
            @Override
            public void onStart() {
            }

            @Override
            public void onStdOut(String line) {
                LOG.info("Stdout:" + line);
            }

            @Override
            public void onStdErr(String line) {
                LOG.info("Stderr:" + line);
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

    /**
     * Implementation that allows access to the environment argument.
     */
    private static class TestExecutor extends ProcessProvidingExecutor {
        private Map<String, String> environment = Map.of();

        @Override
        protected Process launch(CommandLine command, Map<String, String> env, File dir) throws IOException {
            this.environment = env;
            return super.launch(command, env, dir);
        }

        public Map<String, String> getEnvironment() {
            return environment;
        }
    }
}
