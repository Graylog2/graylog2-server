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
package org.graylog2.inputs;

import org.graylog2.inputs.persistence.InputStateService;
import org.graylog2.plugin.IOState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InputRuntimeStatusProviderTest {

    private static final String AUTH_TOKEN = "test-token";

    private InputRuntimeStatusProvider provider;

    @Mock
    private InputStateService runtimeStateService;

    @Mock
    private InputService inputService;

    @BeforeEach
    void setUp() {
        provider = new InputRuntimeStatusProvider(runtimeStateService, inputService);
    }

    @Test
    void returnsFieldNameCorrectly() {
        assertEquals("runtime_status", provider.getFieldName());
    }

    @Test
    void returnsEmptySetForInvalidStatus() {
        final Set<String> result = provider.getMatchingIds("NOT_A_VALID_STATUS", AUTH_TOKEN);
        assertTrue(result.isEmpty());
    }

    @Test
    void runningGroupQueriesMongoDb() {
        when(runtimeStateService.getByState(IOState.Type.RUNNING))
                .thenReturn(Set.of("input-1", "input-3"));

        final Set<String> running = provider.getMatchingIds("RUNNING", AUTH_TOKEN);
        assertEquals(Set.of("input-1", "input-3"), running);
    }

    @Test
    void failedGroupMatchesFailingAndInvalidConfiguration() {
        when(runtimeStateService.getByState(IOState.Type.FAILED))
                .thenReturn(Set.of("input-4"));
        when(runtimeStateService.getByState(IOState.Type.FAILING))
                .thenReturn(Set.of("input-1"));
        when(runtimeStateService.getByState(IOState.Type.INVALID_CONFIGURATION))
                .thenReturn(Set.of("input-2"));

        final Set<String> failed = provider.getMatchingIds("FAILED", AUTH_TOKEN);
        assertEquals(Set.of("input-1", "input-2", "input-4"), failed);
    }

    @Test
    void notRunningGroupQueriesDbForStoppedDesiredState() {
        when(inputService.findIdsByDesiredState(IOState.Type.STOPPED))
                .thenReturn(Set.of("input-1", "input-5"));

        final Set<String> notRunning = provider.getMatchingIds("NOT_RUNNING", AUTH_TOKEN);
        assertEquals(Set.of("input-1", "input-5"), notRunning);

        // Should NOT query runtime state service for NOT_RUNNING
        verify(runtimeStateService, never()).getByState(any());
    }

    @Test
    void setupGroupMatchesSetupInitializedStarting() {
        when(runtimeStateService.getByState(IOState.Type.SETUP))
                .thenReturn(Set.of("input-1"));
        when(runtimeStateService.getByState(IOState.Type.INITIALIZED))
                .thenReturn(Set.of("input-2"));
        when(runtimeStateService.getByState(IOState.Type.STARTING))
                .thenReturn(Set.of("input-3"));

        final Set<String> setup = provider.getMatchingIds("SETUP", AUTH_TOKEN);
        assertEquals(Set.of("input-1", "input-2", "input-3"), setup);
    }

    @Test
    void handlesUpperCaseAndLowerCaseStatus() {
        assertTrue(provider.getMatchingIds("invalid", AUTH_TOKEN).isEmpty());
        assertTrue(provider.getMatchingIds("INVALID", AUTH_TOKEN).isEmpty());
    }

    @Test
    void runningGroupDoesNotQueryDb() {
        when(runtimeStateService.getByState(IOState.Type.RUNNING))
                .thenReturn(Set.of("input-1"));

        provider.getMatchingIds("RUNNING", AUTH_TOKEN);
        verify(inputService, never()).findIdsByDesiredState(any());
    }
}
