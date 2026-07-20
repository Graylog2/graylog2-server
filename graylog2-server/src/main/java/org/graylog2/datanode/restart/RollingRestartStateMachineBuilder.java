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
package org.graylog2.datanode.restart;

import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.StateMachineConfig;

/**
 * Defines the valid transitions between RollingRestartStates.
 *
 * Pure transition validator — no side effects. Side effects are dispatched by
 * {@link RollingRestartService#advance} based on the observed current state.
 */
public final class RollingRestartStateMachineBuilder {

    private RollingRestartStateMachineBuilder() {
    }

    public static StateMachineConfig<RollingRestartState, RollingRestartTrigger> configure() {
        final StateMachineConfig<RollingRestartState, RollingRestartTrigger> c = new StateMachineConfig<>();

        c.configure(RollingRestartState.PREPARING_CLUSTER)
                .permit(RollingRestartTrigger.PROCEED, RollingRestartState.SELECTING_NEXT_NODE)
                .permit(RollingRestartTrigger.ERROR, RollingRestartState.FAILED);

        c.configure(RollingRestartState.SELECTING_NEXT_NODE)
                .permit(RollingRestartTrigger.MORE_NODES, RollingRestartState.UPGRADING_NODE)
                .permit(RollingRestartTrigger.NO_MORE_NODES, RollingRestartState.FINALIZING)
                .permit(RollingRestartTrigger.ABORT, RollingRestartState.ABORTED)
                .permit(RollingRestartTrigger.ERROR, RollingRestartState.FAILED);

        c.configure(RollingRestartState.UPGRADING_NODE)
                .permit(RollingRestartTrigger.PROCEED, RollingRestartState.WAITING_NODE_JOINED)
                .permit(RollingRestartTrigger.ERROR, RollingRestartState.FAILED);

        c.configure(RollingRestartState.WAITING_NODE_JOINED)
                .permit(RollingRestartTrigger.NODE_JOINED, RollingRestartState.REENABLING_ALLOCATION)
                .permit(RollingRestartTrigger.ERROR, RollingRestartState.FAILED);

        c.configure(RollingRestartState.REENABLING_ALLOCATION)
                .permit(RollingRestartTrigger.PROCEED, RollingRestartState.WAITING_GREEN)
                .permit(RollingRestartTrigger.ERROR, RollingRestartState.FAILED);

        c.configure(RollingRestartState.WAITING_GREEN)
                .permit(RollingRestartTrigger.CLUSTER_GREEN, RollingRestartState.SELECTING_NEXT_NODE)
                .permit(RollingRestartTrigger.GREEN_TIMEOUT, RollingRestartState.PAUSED_WAITING_GREEN)
                .permit(RollingRestartTrigger.ERROR, RollingRestartState.FAILED);

        c.configure(RollingRestartState.PAUSED_WAITING_GREEN)
                .permit(RollingRestartTrigger.RESUME, RollingRestartState.WAITING_GREEN);

        c.configure(RollingRestartState.FINALIZING)
                .permit(RollingRestartTrigger.PROCEED, RollingRestartState.COMPLETED)
                .permit(RollingRestartTrigger.ERROR, RollingRestartState.FAILED);

        // Terminal states (COMPLETED, ABORTED, FAILED) have no outgoing transitions.

        return c;
    }

    public static StateMachine<RollingRestartState, RollingRestartTrigger> buildFromState(RollingRestartState initial) {
        return new StateMachine<>(initial, configure());
    }
}
