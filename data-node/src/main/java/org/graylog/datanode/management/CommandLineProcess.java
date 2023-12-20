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
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.graylog.datanode.process.ProcessInformation;
import org.graylog.datanode.process.WatchdogWithProcessInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.constraints.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;


class CommandLineProcess {
    private final Path executable;
    private final List<String> arguments;
    private final ProcessListener listener;
    private final Environment environment;
    private final WatchdogWithProcessInfo watchDog;

    CommandLineProcess(Path executable,
                       List<String> arguments,
                       ProcessListener listener,
                       Environment environment) {
        this.executable = executable;
        this.arguments = arguments;
        this.listener = listener;
        this.environment = environment;
        this.watchDog = new WatchdogWithProcessInfo(ExecuteWatchdog.INFINITE_TIMEOUT);
    }

    private static final Logger LOG = LoggerFactory.getLogger(CommandLineProcess.class);

    public void start() {
        LOG.info("Running process from " + executable.toAbsolutePath());

        CommandLine cmdLine = new CommandLine(executable.toAbsolutePath().toString());
        arguments.forEach(it -> cmdLine.addArgument(it, true));

        try {
            createExecutor().execute(cmdLine, environment.getEnv(), listener);
            listener.onStart();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private DefaultExecutor createExecutor() {
        DefaultExecutor executor = new DefaultExecutor();
        executor.setStreamHandler(new PumpStreamHandler(new LoggingOutputStream(listener::onStdOut), new LoggingOutputStream(listener::onStdErr)));
        executor.setWatchdog(watchDog);
        return executor;
    }

    public void stop() {
        this.watchDog.destroyProcess();
    }

    @NotNull
    public ProcessInformation processInfo() {
        return watchDog.processInfo();
    }
}
