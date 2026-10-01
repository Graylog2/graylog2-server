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

import com.google.common.eventbus.EventBus;
import org.assertj.core.api.Assertions;
import org.graylog.datanode.opensearch.OpensearchStartRequestedEvent;
import org.graylog.datanode.opensearch.statemachine.OpensearchEvent;
import org.graylog.datanode.opensearch.statemachine.OpensearchState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OpensearchWatchdogTracerTest {

    @Mock
    EventBus eventBus;

    @Test
    void testLifecycle() {
        final OpensearchWatchdog watchdog = new OpensearchWatchdog(eventBus, 3);
        watchdog.transition(OpensearchEvent.PROCESS_STARTED, OpensearchState.WAITING_FOR_CONFIGURATION, OpensearchState.STARTING);

        // the process is running now. Let's terminate it and see if the watchdog requests a restart
        terminateProcess(watchdog);
        verify(eventBus, times(1)).post(new OpensearchStartRequestedEvent());

        // repeat
        terminateProcess(watchdog);
        verify(eventBus, times(2)).post(new OpensearchStartRequestedEvent());

        terminateProcess(watchdog);
        verify(eventBus, times(3)).post(new OpensearchStartRequestedEvent());

        // this is the 4th termination, we give up trying, no additional restart is requested
        terminateProcess(watchdog);
        verify(eventBus, times(3)).post(new OpensearchStartRequestedEvent());

        Assertions.assertThat(watchdog.isActive()).isFalse();
    }

    private void terminateProcess(OpensearchWatchdog watchdog) {
        watchdog.transition(OpensearchEvent.PROCESS_TERMINATED, OpensearchState.STARTING, OpensearchState.TERMINATED);
    }
}
