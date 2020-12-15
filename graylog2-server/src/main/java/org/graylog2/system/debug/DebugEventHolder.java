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

public class DebugEventHolder {
    private static volatile DebugEvent clusterEvent;
    private static volatile DebugEvent localEvent;

    public static DebugEvent getClusterDebugEvent() {
        return clusterEvent;
    }

    public static DebugEvent getLocalDebugEvent() {
        return localEvent;
    }

    public static void setClusterDebugEvent(DebugEvent event) {
        DebugEventHolder.clusterEvent = event;
    }

    public static void setLocalDebugEvent(DebugEvent event) {
        DebugEventHolder.localEvent = event;
    }
}
