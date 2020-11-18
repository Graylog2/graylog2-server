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
package org.graylog2.alerts;

import org.joda.time.DateTime;

import java.util.Map;

public interface Alert {
    String getId();
    String getStreamId();
    String getConditionId();
    DateTime getTriggeredAt();
    DateTime getResolvedAt();
    String getDescription();
    Map<String, Object> getConditionParameters();
    boolean isInterval();

    enum AlertState {
        ANY, RESOLVED, UNRESOLVED;

        public static AlertState fromString(String state) {
            for (AlertState aState : AlertState.values()) {
                if (aState.toString().equalsIgnoreCase(state)) {
                    return aState;
                }
            }

            throw new IllegalArgumentException("Alert state " + state + " is not supported");
        }
    }
}
