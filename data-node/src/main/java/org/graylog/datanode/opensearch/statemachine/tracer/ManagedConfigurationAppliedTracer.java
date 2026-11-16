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
package org.graylog.datanode.opensearch.statemachine.tracer;

import jakarta.inject.Inject;
import org.graylog.datanode.opensearch.statemachine.OpensearchEvent;
import org.graylog.datanode.opensearch.statemachine.OpensearchState;
import org.graylog.datanode.process.statemachine.tracer.StateMachineTracer;
import org.graylog2.configuration.managed.ManagedConfigurationApplier;

/**
 * Records that the opensearch process runs with the configuration values which were loaded for its start. Only then
 * do stored values stop counting as waiting for a restart of this data node.
 */
public class ManagedConfigurationAppliedTracer implements StateMachineTracer<OpensearchState, OpensearchEvent> {
    private final ManagedConfigurationApplier managedConfigurationApplier;

    @Inject
    public ManagedConfigurationAppliedTracer(ManagedConfigurationApplier managedConfigurationApplier) {
        this.managedConfigurationApplier = managedConfigurationApplier;
    }

    @Override
    public void trigger(OpensearchEvent trigger) {
    }

    @Override
    public void transition(OpensearchEvent trigger, OpensearchState source, OpensearchState destination) {
        if (source != destination && destination == OpensearchState.AVAILABLE) {
            managedConfigurationApplier.processAvailable();
        }
    }
}
