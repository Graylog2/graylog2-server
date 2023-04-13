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

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.graylog.datanode.ProcessProvidingExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;


class CommandLineProcess {
    private static final String JAVA_HOME_ENV = "JAVA_HOME";

    private final Path executable;
    private final List<String> arguments;
    private final ProcessListener listener;
    private final Supplier<ProcessProvidingExecutor> executorSupplier;
    private final Supplier<Map<String, String>> environmentSupplier;
    private final ExecuteWatchdog watchDog;

    private Process process;

    public CommandLineProcess(Path executable, List<String> arguments, ProcessListener listener) {
        this(executable, arguments, listener, ProcessProvidingExecutor::new, System::getenv);
    }

    @VisibleForTesting
    CommandLineProcess(Path executable,
                       List<String> arguments,
                       ProcessListener listener,
                       Supplier<ProcessProvidingExecutor> executorSupplier,
                       Supplier<Map<String, String>> environmentSupplier) {
        this.executable = executable;
        this.arguments = arguments;
        this.listener = listener;
        this.environmentSupplier = environmentSupplier;
        this.executorSupplier = executorSupplier;
        this.watchDog = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);
    }

    private static final Logger LOG = LoggerFactory.getLogger(CommandLineProcess.class);

    public void start() throws IOException {

        LOG.info("Running process from " + executable.toAbsolutePath());

        CommandLine cmdLine = new CommandLine(executable.toAbsolutePath().toString());

        arguments.forEach(it -> cmdLine.addArgument(it, true));

        ProcessProvidingExecutor executor = executorSupplier.get();

        //executor.setStreamHandler(logs);
        executor.setStreamHandler(new PumpStreamHandler(new LoggingOutputStream(listener::onStdOut), new LoggingOutputStream(listener::onStdErr)));
        executor.setWatchdog(watchDog);
        executor.execute(cmdLine, cleanEnvironment(environmentSupplier.get()), listener);
        try {
            this.process = executor.getProcess().get(30, TimeUnit.SECONDS);
            listener.onStart();
        } catch (TimeoutException e) {
            throw new RuntimeException("Failed to obtain process", e);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> cleanEnvironment(Map<String, String> env) {
        return env.entrySet().stream()
                // Remove JAVA_HOME from environment because OpenSearch should use its bundled JVM.
                .filter(not(entry -> JAVA_HOME_ENV.equals(entry.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void stop() {
        this.watchDog.destroyProcess();
    }

    /**
     * "Do not rely on the undelying process if not necessary"
     */
    @Deprecated(forRemoval = true)
    public Optional<Process> getProcess() {
        return Optional.ofNullable(process);
    }
}
