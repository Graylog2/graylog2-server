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

import org.graylog.datanode.process.OpensearchConfiguration;
import org.graylog.datanode.process.ProcessInformation;

import javax.validation.constraints.NotNull;
import java.io.Closeable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

public class OpensearchCommandLineProcess implements Closeable {

    private final CommandLineProcess commandLineProcess;

    public OpensearchCommandLineProcess(OpensearchConfiguration config, ProcessListener listener) {
        final Path executable = config.opensearchDir().resolve(Paths.get("bin", "opensearch"));
        final List<String> arguments = config.asMap().entrySet().stream()
                .map(it -> String.format(Locale.ROOT, "-E%s=%s", it.getKey(), it.getValue())).toList();
        commandLineProcess = new CommandLineProcess(executable, arguments, listener, config.getEnv());
    }

    public void start() {
        commandLineProcess.start();
    }

    @Override
    public void close() {
        commandLineProcess.stop();
    }

    @NotNull
    public ProcessInformation processInfo() {
        return commandLineProcess.processInfo();
    }
}
