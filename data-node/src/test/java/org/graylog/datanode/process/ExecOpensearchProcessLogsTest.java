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
package org.graylog.datanode.process;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import org.assertj.core.api.Assertions;
import org.graylog.datanode.management.LogsCache;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

class ExecOpensearchProcessLogsTest {

    @Test
    void testLogging() throws IOException, ExecutionException, RetryException {

        final ByteArrayInputStream stdout = new ByteArrayInputStream("stdout-line".getBytes(StandardCharsets.UTF_8));
        final ByteArrayInputStream stderr = new ByteArrayInputStream("stderr-line".getBytes(StandardCharsets.UTF_8));

        final LogsCache logger = new LogsCache(10);
        logger.setProcessOutputStream(stdout);
        logger.setProcessErrorStream(stderr);


        logger.start();

        final Retryer<List<String>> retryer = RetryerBuilder.<List<String>>newBuilder()
                .withWaitStrategy(WaitStrategies.fixedWait(100, TimeUnit.MILLISECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(20))
                .retryIfResult(List::isEmpty)
                .build();

        final List<String> stdOutLogs = retryer.call(logger::stdOut);
        final List<String> stdErrLogs = retryer.call(logger::stdErr);

        Assertions.assertThat(stdOutLogs)
                .hasSize(1)
                .containsExactly("stdout-line");

        Assertions.assertThat(stdErrLogs)
                .hasSize(1)
                .containsExactly("stderr-line");

        Assertions.assertThatCode(logger::stop)
                .doesNotThrowAnyException();

    }
}
