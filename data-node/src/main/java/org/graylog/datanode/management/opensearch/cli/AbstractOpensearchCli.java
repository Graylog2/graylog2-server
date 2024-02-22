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
package org.graylog.datanode.management.opensearch.cli;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class AbstractOpensearchCli {

    private final Path configPath;
    private final Path binPath;

    public AbstractOpensearchCli(Path configPath, Path bin) {
        this.configPath = configPath;
        this.binPath = bin;
    }

    protected String run(String... args) {
        return run(Collections.emptyList(), args);
    }

    protected String run(List<String> answersToPrompts, String... args) {
        final CommandLine cmd = new CommandLine(binPath.toFile());
        Arrays.asList(args).forEach(cmd::addArgument);

        final ByteArrayOutputStream stdout = new ByteArrayOutputStream();

        final PumpStreamHandler pumpStreamHandler;
        if (answersToPrompts.isEmpty()) {
            pumpStreamHandler = new PumpStreamHandler(stdout, stdout);
        } else {
            InputStream input = new ByteArrayInputStream(String.join("\n", answersToPrompts).getBytes(Charset.defaultCharset()));
            pumpStreamHandler = new PumpStreamHandler(stdout, stdout, input);
        }

        final DefaultExecutor executor = new DefaultExecutor.Builder<>()
                .setExecuteStreamHandler(pumpStreamHandler)
                .get();

        try {
            final DefaultExecuteResultHandler executeResultHandler = new DefaultExecuteResultHandler();
            final Map<String, String> env = Collections.singletonMap("OPENSEARCH_PATH_CONF", configPath.toAbsolutePath().toString());
            executor.execute(cmd, env, executeResultHandler);
            executeResultHandler.waitFor();
            final int exitValue = executeResultHandler.getExitValue();
            if (exitValue != 0) {
                throw new RuntimeException("Failed to execute opensearch cli" + binPath + "\n" + formatOutput(stdout));
            }
            return stdout.toString(StandardCharsets.UTF_8).trim();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to execute opensearch cli" + binPath + "\n" + formatOutput(stdout));
        }
    }

    private String formatOutput(ByteArrayOutputStream stdout) {
        return "STDOUT/STDERR: " + stdout.toString(StandardCharsets.UTF_8);
    }
}
