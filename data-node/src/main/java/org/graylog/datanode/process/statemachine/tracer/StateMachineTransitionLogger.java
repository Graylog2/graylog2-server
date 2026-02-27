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
package org.graylog.datanode.process.statemachine.tracer;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StateMachineTransitionLogger<STATE, EVENT> implements StateMachineTracer<STATE, EVENT> {

    private static final Logger LOG = LoggerFactory.getLogger(StateMachineTransitionLogger.class);

    @Inject
    public StateMachineTransitionLogger() {
    }

    @Override
    public void trigger(EVENT trigger) {

    }

    @Override
    public void transition(EVENT trigger, STATE source, STATE destination) {
        if (!source.equals(destination)) {
            LOG.debug("Triggered {}, source state: {}, destination: {}", trigger, source, destination);
        }
    }

}
