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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.graylog2.datanode.restart.RollingRestartState.ABORTED;
import static org.graylog2.datanode.restart.RollingRestartState.COMPLETED;
import static org.graylog2.datanode.restart.RollingRestartState.FAILED;
import static org.graylog2.datanode.restart.RollingRestartState.FINALIZING;
import static org.graylog2.datanode.restart.RollingRestartState.PAUSED_WAITING_GREEN;
import static org.graylog2.datanode.restart.RollingRestartState.PREPARING_CLUSTER;
import static org.graylog2.datanode.restart.RollingRestartState.REENABLING_ALLOCATION;
import static org.graylog2.datanode.restart.RollingRestartState.SELECTING_NEXT_NODE;
import static org.graylog2.datanode.restart.RollingRestartState.UPGRADING_NODE;
import static org.graylog2.datanode.restart.RollingRestartState.WAITING_GREEN;
import static org.graylog2.datanode.restart.RollingRestartState.WAITING_NODE_JOINED;
import static org.graylog2.datanode.restart.RollingRestartTrigger.ABORT;
import static org.graylog2.datanode.restart.RollingRestartTrigger.CLUSTER_GREEN;
import static org.graylog2.datanode.restart.RollingRestartTrigger.ERROR;
import static org.graylog2.datanode.restart.RollingRestartTrigger.GREEN_TIMEOUT;
import static org.graylog2.datanode.restart.RollingRestartTrigger.MORE_NODES;
import static org.graylog2.datanode.restart.RollingRestartTrigger.NODE_JOINED;
import static org.graylog2.datanode.restart.RollingRestartTrigger.NODE_LEFT;
import static org.graylog2.datanode.restart.RollingRestartTrigger.NO_MORE_NODES;
import static org.graylog2.datanode.restart.RollingRestartTrigger.PROCEED;
import static org.graylog2.datanode.restart.RollingRestartTrigger.RESUME;

class RollingRestartStateMachineBuilderTest {

    private static StateMachine<RollingRestartState, RollingRestartTrigger> from(RollingRestartState state) {
        return RollingRestartStateMachineBuilder.buildFromState(state);
    }

    @Test
    void preparingCluster_proceed_goesToSelectingNextNode() {
        final var sm = from(PREPARING_CLUSTER);
        sm.fire(PROCEED);
        assertThat(sm.getState()).isEqualTo(SELECTING_NEXT_NODE);
    }

    @Test
    void selectingNextNode_moreNodes_goesToUpgradingNode() {
        final var sm = from(SELECTING_NEXT_NODE);
        sm.fire(MORE_NODES);
        assertThat(sm.getState()).isEqualTo(UPGRADING_NODE);
    }

    @Test
    void selectingNextNode_noMoreNodes_goesToFinalizing() {
        final var sm = from(SELECTING_NEXT_NODE);
        sm.fire(NO_MORE_NODES);
        assertThat(sm.getState()).isEqualTo(FINALIZING);
    }

    @Test
    void selectingNextNode_abort_goesToAborted() {
        final var sm = from(SELECTING_NEXT_NODE);
        sm.fire(ABORT);
        assertThat(sm.getState()).isEqualTo(ABORTED);
    }

    @Test
    void startingNode_proceed_goesToWaitingNodeJoined() {
        final var sm = from(UPGRADING_NODE);
        sm.fire(PROCEED);
        assertThat(sm.getState()).isEqualTo(WAITING_NODE_JOINED);
    }

    @Test
    void waitingNodeJoined_nodeJoined_goesToReenablingAllocation() {
        final var sm = from(WAITING_NODE_JOINED);
        sm.fire(NODE_JOINED);
        assertThat(sm.getState()).isEqualTo(REENABLING_ALLOCATION);
    }

    @Test
    void reenablingAllocation_proceed_goesToWaitingGreen() {
        final var sm = from(REENABLING_ALLOCATION);
        sm.fire(PROCEED);
        assertThat(sm.getState()).isEqualTo(WAITING_GREEN);
    }

    @Test
    void waitingGreen_clusterGreen_goesToSelectingNextNode() {
        final var sm = from(WAITING_GREEN);
        sm.fire(CLUSTER_GREEN);
        assertThat(sm.getState()).isEqualTo(SELECTING_NEXT_NODE);
    }

    @Test
    void waitingGreen_greenTimeout_goesToPausedWaitingGreen() {
        final var sm = from(WAITING_GREEN);
        sm.fire(GREEN_TIMEOUT);
        assertThat(sm.getState()).isEqualTo(PAUSED_WAITING_GREEN);
    }

    @Test
    void pausedWaitingGreen_resume_goesToWaitingGreen() {
        final var sm = from(PAUSED_WAITING_GREEN);
        sm.fire(RESUME);
        assertThat(sm.getState()).isEqualTo(WAITING_GREEN);
    }

    @Test
    void finalizing_proceed_goesToCompleted() {
        final var sm = from(FINALIZING);
        sm.fire(PROCEED);
        assertThat(sm.getState()).isEqualTo(COMPLETED);
    }

    @Test
    void anyNonTerminalState_acceptsError() {
        for (RollingRestartState state : RollingRestartState.values()) {
            if (state.isTerminal() || state == PAUSED_WAITING_GREEN) {
                continue;
            }
            final var sm = from(state);
            sm.fire(ERROR);
            assertThat(sm.getState())
                    .as("ERROR from %s should land in FAILED", state)
                    .isEqualTo(FAILED);
        }
    }

    @Test
    void selectingNextNode_invalidTrigger_throws() {
        final var sm = from(SELECTING_NEXT_NODE);
        // Triggers not permitted from SELECTING_NEXT_NODE
        assertThatThrownBy(() -> sm.fire(NODE_LEFT)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> sm.fire(CLUSTER_GREEN)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> sm.fire(RESUME)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void terminalStates_rejectAllTriggers() {
        for (RollingRestartState terminal : new RollingRestartState[]{COMPLETED, ABORTED, FAILED}) {
            for (RollingRestartTrigger trigger : RollingRestartTrigger.values()) {
                final var sm = from(terminal);
                assertThatThrownBy(() -> sm.fire(trigger))
                        .as("Terminal state %s should reject trigger %s", terminal, trigger)
                        .isInstanceOf(IllegalStateException.class);
            }
        }
    }

    @Test
    void waitingGreen_doesNotAcceptAbortDirectly() {
        // Abort is only honoured at SELECTING_NEXT_NODE — fail-fast if the executor ever fires ABORT
        // from another state (this would indicate a bug, not a user action).
        final var sm = from(WAITING_GREEN);
        assertThatThrownBy(() -> sm.fire(ABORT)).isInstanceOf(IllegalStateException.class);
    }
}
