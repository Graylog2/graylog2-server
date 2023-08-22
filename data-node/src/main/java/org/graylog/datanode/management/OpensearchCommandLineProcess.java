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

import org.apache.commons.exec.OS;
import org.graylog.datanode.process.OpensearchConfiguration;
import org.graylog.datanode.process.ProcessInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

public class OpensearchCommandLineProcess implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(OpensearchCommandLineProcess.class);

    private final CommandLineProcess commandLineProcess;
    private final CommandLineProcessListener resultHandler;

    /**
     * as long as OpenSearch is not supported on macOS, we have to fix the jdk path if we want to
     * start the DataNode inside IntelliJ.
     * @param config
     */
    private void fixJdkOnMac(final OpensearchConfiguration config) {
        final var isMacOS = OS.isFamilyMac();
        final var jdk = config.opensearchDir().resolve("jdk.app");
        final var jdkNotLinked = !Files.exists(jdk);
        if (isMacOS && jdkNotLinked) {
            // Link System jdk into startup folder, get path:
            final ProcessBuilder builder = new ProcessBuilder("/usr/libexec/java_home");
            builder.redirectErrorStream(true);
            try {
                final Process process = builder.start();
                final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.defaultCharset()));
                var line = reader.readLine();
                if(line != null && Files.exists(Path.of(line))) {
                    final var target = Path.of(line);
                    final var src = Files.createDirectories(jdk.resolve("Contents"));
                    Files.createSymbolicLink(src.resolve("Home"), target);
                } else {
                    LOG.error("Output of '/usr/libexec/java_home' is not the jdk: {}", line);
                }
                // cleanup
                process.destroy();
                reader.close();
            } catch (IOException e) {
                LOG.error("Could not link jdk.app on macOS: {}", e.getMessage(), e);
            }
        }
    }

    public OpensearchCommandLineProcess(OpensearchConfiguration config, ProcessListener listener) {
        fixJdkOnMac(config);
        final Path executable = config.opensearchDir().resolve(Paths.get("bin", "opensearch"));
        final List<String> arguments = config.asMap().entrySet().stream()
                .map(it -> String.format(Locale.ROOT, "-E%s=%s", it.getKey(), it.getValue())).toList();
        resultHandler = new CommandLineProcessListener(listener);
        commandLineProcess = new CommandLineProcess(executable, arguments, resultHandler, config.getEnv());
    }

    public void start() {
        commandLineProcess.start();
    }

    @Override
    public void close() {
        commandLineProcess.stop();
        resultHandler.stopListening();

    }

    @NotNull
    public ProcessInformation processInfo() {
        return commandLineProcess.processInfo();
    }
}
