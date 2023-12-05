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
import org.graylog.datanode.process.StateMachineTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StateMachineTransitionLogger implements StateMachineTracer {

    private static final Logger LOG = LoggerFactory.getLogger(StateMachineTransitionLogger.class);

    @Override
    public void trigger(ProcessEvent trigger) {

    }

    @Override
    public void transition(ProcessEvent trigger, ProcessState source, ProcessState destination) {
        if (!source.equals(destination)) {
            LOG.debug("Triggered {}, source state: {}, destination: {}", trigger, source, destination);
        }
    }
}
