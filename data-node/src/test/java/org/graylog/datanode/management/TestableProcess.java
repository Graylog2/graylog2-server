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

import org.graylog.datanode.process.ProcessEvent;
import org.graylog.datanode.process.ProcessState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestableProcess implements ManagableProcess {

    private static final Logger LOG = LoggerFactory.getLogger(TestableProcess.class);

    private volatile ProcessState state;

    public TestableProcess() {
        this.state = ProcessState.NEW;
    }

    @Override
    public void start() {
        LOG.debug("Starting process");
        this.state = ProcessState.STARTING;
    }

    @Override
    public void stop() {
        LOG.debug("Stopping process process");
        this.state = ProcessState.TERMINATED;
    }

    @Override
    public void onEvent(ProcessEvent event) {
        // ignored
    }

    @Override
    public boolean isInState(ProcessState state) {
        return this.state == state;
    }
}
