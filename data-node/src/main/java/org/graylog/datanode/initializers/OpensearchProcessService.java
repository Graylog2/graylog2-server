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
package org.graylog.datanode.initializers;

import com.github.rholder.retry.RetryException;
import com.google.common.util.concurrent.AbstractIdleService;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteResultHandler;
import org.graylog.datanode.EventResultHandler;
import org.graylog.datanode.ProcessProvidingExecutor;
import org.graylog.datanode.process.ExecOpensearchProcessLogs;
import org.graylog.datanode.process.OpensearchConfiguration;
import org.graylog.datanode.process.OpensearchProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

@Singleton
public class OpensearchProcessService extends AbstractIdleService implements Provider<OpensearchProcess> {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchProcessService.class);
    private final OpensearchConfiguration config;

    private final OpensearchProcess process;

    @Inject
    public OpensearchProcessService(OpensearchConfiguration config, @Named(value = "process_logs_buffer_size") int logsSize) {
        this.config = config;
        final ExecOpensearchProcessLogs logger = new ExecOpensearchProcessLogs(logsSize);
        this.process = new OpensearchProcess(
                config.opensearchVersion(),
                config.opensearchDir(),
                logger,
                config.httpPort(),
                config.clusterConfiguration().nodeName()
        );
    }

    @Override
    protected void startUp() throws Exception {
        doStartProcess();
    }

    private void doStartProcess() throws IOException, InterruptedException, ExecutionException {
        final Path binPath = config.opensearchDir().resolve(Paths.get("bin", "opensearch"));
        LOG.info("Running opensearch from " + binPath.toAbsolutePath());

        CommandLine cmdLine = new CommandLine(binPath.toAbsolutePath().toString());

        toConfigOptions(config.mergedConfig())
                .forEach(it -> cmdLine.addArgument(it, true));

        ProcessProvidingExecutor executor = new ProcessProvidingExecutor();
        ExecuteResultHandler resultHandler = new EventResultHandler(this.process);
        executor.setStreamHandler(this.process.getProcessLogs());
        executor.execute(cmdLine, resultHandler);

        final Process process;
        try {
            process = executor.getProcess().get(5, TimeUnit.SECONDS);
            this.process.bind(process);
        } catch (TimeoutException e) {
            throw new RuntimeException("Failed to obtain process", e);
        }
    }

    private Stream<String> toConfigOptions(Map<String, String> mergedConfig) {
        return mergedConfig.entrySet().stream()
                .map(it -> String.format(Locale.ROOT, "-E%s=%s", it.getKey(), it.getValue()));
    }


    @Override
    protected void shutDown() {
        this.process.terminate();
    }

    @Override
    public OpensearchProcess get() {
        return process;
    }
}
