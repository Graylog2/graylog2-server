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
package org.graylog.datanode.process;

import org.apache.commons.exec.ExecuteWatchdog;

import jakarta.validation.constraints.NotNull;

import java.util.Optional;

public class WatchdogWithProcessInfo extends ExecuteWatchdog {

    private Process process;

    public WatchdogWithProcessInfo(long timeout) {
        super(timeout);
    }

    @Override
    public synchronized void start(Process processToMonitor) {
        super.start(processToMonitor);
        this.process = processToMonitor;
    }

    @NotNull
    public ProcessInformation processInfo() {
        return Optional.ofNullable(process)
                .map(ProcessInformation::create)
                .orElse(ProcessInformation.empty());
    }

}
