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
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.graylog.datanode.process.OpensearchProcess;
import org.graylog.datanode.process.OpensearchProcessLogs;
import org.graylog.datanode.process.OpensearchConfiguration;
import org.graylog.datanode.process.ProcessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
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
            return run(opensearchConfiguration);
        } catch (IOException | InterruptedException | ExecutionException | RetryException e) {
            throw new RuntimeException(e);
        }
    }

    private OpensearchProcess run(OpensearchConfiguration config) throws IOException, InterruptedException, ExecutionException, RetryException {

        final Path binPath = config.opensearchDir().resolve(Paths.get("bin", "opensearch"));
        LOG.info("Running opensearch from " + binPath.toAbsolutePath());

        List<String> command = new ArrayList<>();
        command.add(binPath.toAbsolutePath().toString());
        command.addAll(toConfigOptions(config.mergedConfig()));
        ProcessBuilder builder = new ProcessBuilder(command);

        // TODO: why is this not working?
        //builder.environment().putAll(config.mergedConfig());

        final Process process = builder.start();
        final OpensearchProcessLogs logs = OpensearchProcessLogs.createFor(process, logsSize);
        final OpensearchProcess opensearchProcess = new OpensearchProcess(config.opensearchVersion(), config.opensearchDir(), process, logs, config.httpPort());
        opensearchProcess.onEvent(ProcessEvent.PROCESS_STARTED);
        return opensearchProcess;
    }

    /**
     * TODO: this could be potentially very dangerous - with a properly formatted config the user could
     * execute anything in the underlying system!
     */
    private List<String> toConfigOptions(Map<String, String> mergedConfig) {
        return mergedConfig.entrySet().stream()
                .map(it -> "-E" + it.getKey() + "=" + it.getValue())
                .collect(Collectors.toList());
    }
}
