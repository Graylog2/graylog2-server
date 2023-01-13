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

import org.graylog.datanode.process.OpensearchProcessLogs;

import java.nio.file.Path;

public class OpensearchProcess {
    private String opensearchVersion;
    private Path targetLocation;
    private final Process process;
    private OpensearchProcessLogs processLogs;

    protected OpensearchProcess(String opensearchVersion, Path targetLocation, Process opensearchProcess, OpensearchProcessLogs processLogs) {
        this.opensearchVersion = opensearchVersion;
        this.targetLocation = targetLocation;
        this.process = opensearchProcess;
        this.processLogs = processLogs;
    }

    public Process getProcess() {
        return process;
    }

    public String getOpensearchVersion() {
        return opensearchVersion;
    }

    public Path getTargetLocation() {
        return targetLocation;
    }

    public OpensearchProcessLogs getProcessLogs() {
        return processLogs;
    }
}

