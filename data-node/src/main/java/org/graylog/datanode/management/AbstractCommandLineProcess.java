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

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteResultHandler;
import org.graylog.datanode.EventResultHandler;
import org.graylog.datanode.ProcessProvidingExecutor;
import org.graylog.datanode.process.ExecOpensearchProcessLogs;
import org.graylog.datanode.process.ProcessEvent;
import org.graylog.datanode.process.ProcessLogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class AbstractCommandLineProcess implements OpensearchProcess {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractCommandLineProcess.class);
    private Process process;
    private ExecOpensearchProcessLogs logger;

    protected abstract Path getExecutable();

    protected abstract List<String> getCommandLineArguments();

    protected abstract int getLogsSize();

    protected Optional<Process> process() {
        return Optional.ofNullable(process);
    }

    @Override
    public Optional<ProcessLogs> processLogs() {
        return Optional.ofNullable(logger);
    }

    @Override
    public void start() throws IOException {

        final Path binPath = getExecutable();
        LOG.info("Running opensearch from " + binPath.toAbsolutePath());

        CommandLine cmdLine = new CommandLine(binPath.toAbsolutePath().toString());

        getCommandLineArguments().forEach(it -> cmdLine.addArgument(it, true));

        ProcessProvidingExecutor executor = new ProcessProvidingExecutor();
        logger = new ExecOpensearchProcessLogs(getLogsSize());
        ExecuteResultHandler resultHandler = new EventResultHandler(this);
        executor.setStreamHandler(logger);
        executor.execute(cmdLine, resultHandler);

        try {
            process = executor.getProcess().get(5, TimeUnit.SECONDS);
            onEvent(ProcessEvent.PROCESS_STARTED);
        } catch (TimeoutException e) {
            throw new RuntimeException("Failed to obtain process", e);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        this.process.destroy();
    }
}
