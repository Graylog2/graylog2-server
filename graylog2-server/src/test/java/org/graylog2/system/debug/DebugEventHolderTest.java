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
package org.graylog2.system.debug;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DebugEventHolderTest {
    @Test
    public void setAndGetClusterEventReturnsSameObject() throws Exception {
        DebugEvent event = DebugEvent.create("Node ID", "Test");
        DebugEventHolder.setClusterDebugEvent(event);

        assertThat(DebugEventHolder.getClusterDebugEvent()).isSameAs(event);
    }

    @Test
    public void setAndGetDebugEventReturnsSameObject() throws Exception {
        DebugEvent event = DebugEvent.create("Node ID", "Test");
        DebugEventHolder.setLocalDebugEvent(event);

        assertThat(DebugEventHolder.getLocalDebugEvent()).isSameAs(event);
    }
}