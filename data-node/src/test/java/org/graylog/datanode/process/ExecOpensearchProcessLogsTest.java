package org.graylog.datanode.process;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import org.assertj.core.api.Assertions;
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

        final ExecOpensearchProcessLogs logger = new ExecOpensearchProcessLogs(10);
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
