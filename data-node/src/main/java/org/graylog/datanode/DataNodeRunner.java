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
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class DataNodeRunner {

    final private int logsSize;

    private static final Logger LOG = LoggerFactory.getLogger(DataNodeRunner.class);

    @Inject
    public DataNodeRunner(@Named(value = "process_logs_buffer_size") int logsSize) {
        this.logsSize = logsSize;
    }

    public OpensearchProcess start(OpensearchConfiguration opensearchConfiguration) {
        try {
            setConfiguration(opensearchConfiguration);
            return run(opensearchConfiguration);
        } catch (IOException | InterruptedException | ExecutionException | RetryException e) {
            throw new RuntimeException(e);
        }
    }

    private void setConfiguration(OpensearchConfiguration config) throws IOException {
        final Path configPath = config.opensearchDir().resolve(Path.of("config", "opensearch.yml"));
        File file = configPath.toFile();
        try (
                FileWriter fr = new FileWriter(file, StandardCharsets.UTF_8);
                BufferedWriter br = new BufferedWriter(fr);
        ) {

            for (Map.Entry<String, String> option : config.mergedConfig().entrySet()) {
                final String optionLine = toOptionLine(option);
                LOG.info("Setting configuration: " + option);
                br.write(optionLine + "\n");
            }
        }
    }

    private String toOptionLine(Map.Entry<String, String> option) {
        return option.getKey() + ": " + option.getValue();
    }

    private OpensearchProcess run(OpensearchConfiguration config) throws IOException, InterruptedException, ExecutionException, RetryException {
        final Path binPath = config.opensearchDir().resolve(Paths.get("bin", "opensearch"));
        LOG.info("Running opensearch from " + binPath.toAbsolutePath());
        ProcessBuilder builder = new ProcessBuilder(binPath.toAbsolutePath().toString());

        final Process process = builder.start();
        final OpensearchProcessLogs logs = OpensearchProcessLogs.createFor(process, logsSize);
        final OpensearchProcess opensearchProcess = new OpensearchProcess(config.opensearchVersion(), config.opensearchDir(), process, logs, config.httpPort());
        opensearchProcess.onEvent(ProcessEvent.PROCESS_STARTED);
        return opensearchProcess;
    }
}
