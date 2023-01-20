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
package org.graylog.datanode;

import com.github.rholder.retry.RetryException;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.ExecuteWatchdog;
import org.graylog.datanode.process.ExecOpensearchProcessLogs;
import org.graylog.datanode.process.OpensearchConfiguration;
import org.graylog.datanode.process.OpensearchProcess;
import org.graylog.datanode.process.ProcessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

public class DataNodeRunner {

    final private int logsSize;

    private static final Logger LOG = LoggerFactory.getLogger(DataNodeRunner.class);

    @Inject
    public DataNodeRunner(@Named(value = "process_logs_buffer_size") int logsSize) {
        this.logsSize = logsSize;
    }

    public OpensearchProcess start(OpensearchConfiguration opensearchConfiguration) {
        try {
            return doStartProcess(opensearchConfiguration);
        } catch (IOException | InterruptedException | ExecutionException | RetryException e) {
            throw new RuntimeException(e);
        }
    }

    private OpensearchProcess doStartProcess(OpensearchConfiguration config) throws IOException, InterruptedException, ExecutionException, RetryException {

        final Path binPath = config.opensearchDir().resolve(Paths.get("bin", "opensearch"));
        LOG.info("Running opensearch from " + binPath.toAbsolutePath());

        CommandLine cmdLine = new CommandLine(binPath.toAbsolutePath().toString());

        toConfigOptions(config.mergedConfig())
                .forEach(it -> cmdLine.addArgument(it, true));

        ProcessProvidingExecutor executor = new ProcessProvidingExecutor();
        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
        final ExecOpensearchProcessLogs logger = new ExecOpensearchProcessLogs(logsSize);
        executor.setStreamHandler(logger);
        executor.execute(cmdLine, resultHandler);

        final Process process;
        try {
            process = executor.getProcess().get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("Failed to obtain process", e);
        }
        final OpensearchProcess opensearchProcess = new OpensearchProcess(config.opensearchVersion(), config.opensearchDir(), process, logger, config.httpPort());
        opensearchProcess.onEvent(ProcessEvent.PROCESS_STARTED);
        return opensearchProcess;
    }

    private Stream<String> toConfigOptions(Map<String, String> mergedConfig) {
        return mergedConfig.entrySet().stream()
                .map(it -> String.format(Locale.ROOT, "-E%s=%s", it.getKey(), it.getValue()));
    }
}
