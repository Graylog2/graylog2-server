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
package org.graylog.plugins.views.storage.migration.state.actions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TrafficSnapshotTest {

    @Test
    public void testEstimation() {
        long startTimeStamp = 0L;
        long endTimeStamp = 120000L; // 2 min
        long startBytes = 0L;
        long endBytes = 10000L;
        TrafficSnapshot trafficSnapshot = new TrafficSnapshot(startBytes, startTimeStamp);
        assertThat(trafficSnapshot.calculateEstimatedTrafficPerMinute(endBytes, endTimeStamp))
                .isEqualTo(5000L);
    }

    @Test
    public void testEstimationRounded() {
        long startTimeStamp = 0L;
        long endTimeStamp = 90000L; // 1.5 min
        long startBytes = 0L;
        long endBytes = 10000L;
        TrafficSnapshot trafficSnapshot = new TrafficSnapshot(startBytes, startTimeStamp);
        assertThat(trafficSnapshot.calculateEstimatedTrafficPerMinute(endBytes, endTimeStamp))
                .isEqualTo(6666L);
    }

}
