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
import org.graylog.datanode.process.OpensearchConfiguration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class AbstractOpensearchCli {

    private final Path configPath;
    private final Path binPath;

    /**
     *
     * @param configPath Opensearch CLI tools adapt configuration stored under OPENSEARCH_PATH_CONF env property.
     *                   This is why wealways want to set this configPath for each CLI tool.
     * @param bin location of the actual executable binary that this wrapper handles
     */
    private AbstractOpensearchCli(Path configPath, Path bin) {
        this.configPath = configPath;
        this.binPath = bin;
    }

    public AbstractOpensearchCli(OpensearchConfiguration config, String binName) {
        this(config.datanodeDirectories().getOpensearchProcessConfigurationDir(),
                checkExecutable(config.opensearchDistribution().getOpensearchBinDirPath().resolve(binName)));
    }

    private static Path checkExecutable(Path path) {
        if (!Files.isExecutable(path)) {
            throw new IllegalArgumentException("Path " + path + " doesn't point to any known opensearch cli tool");
        }
        return path;
    }

    protected String runBatch(String... args) {
        return runWithStdin(Collections.emptyList(), args);
    }

    /**
     * @param answersToPrompts Some opensearch CLI tools have to be operated in an interactive way - provide answers
     *                         to questions the tool is asking. It's scripting unfriendly. Our way around this is to
     *                         provide an input stream of expected responses, each delimited by \n. There is no validation
     *                         and no logic, just the expected order of responses.
     * @param args arguments of the command, in opensearch-keystore create, the create is the first argument
     * @return All the STDOUT and STDERR of the process merged into one String.
     */
    protected String runWithStdin(List<String> answersToPrompts, String... args) {
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

        // TODO: add watchdog and timeout to the underlying command handling
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
