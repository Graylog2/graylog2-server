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
package org.graylog2.indexer.retention.executors;

import org.graylog2.indexer.rotation.tso.IndexLifetimeConfig;
import org.joda.time.Period;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TimeBasedRetentionExecutorTest extends AbstractRetentionExecutorTest {

    private TimeBasedRetentionExecutor underTest;

    private static IndexLifetimeConfig getIndexLifetimeConfig(int min, int max) {
        return IndexLifetimeConfig.builder()
                .indexLifetimeMin(Period.days(min))
                .indexLifetimeMax(Period.days(max))
                .build();
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        underTest = new TimeBasedRetentionExecutor(indices, clock, activityWriter, retentionExecutor);
    }

    @Test
    public void retainTimeBased() {
        underTest.retain(indexSet, getIndexLifetimeConfig(10, 12), action, "action");

        verify(action, times(1)).retain(retainedIndexName.capture(), eq(indexSet));
        assertThat(retainedIndexName.getValue()).containsExactly("test_1", "test_2", "test_3");
    }

    @Test
    public void retainTimeBasedNothing() {
        underTest.retain(indexSet, getIndexLifetimeConfig(20, 30), action, "action");

        verify(action, times(0)).retain(any(), any());
    }

    @Test
    public void timeBasedMissingClosingDate() {
        when(indices.indexClosingDate("test_1")).thenReturn(Optional.empty());
        when(indices.indexCreationDate("test_1")).thenReturn(Optional.of(NOW.minusDays(17)));

        underTest.retain(indexSet, getIndexLifetimeConfig(14, 16), action, "action");

        verify(action, times(1)).retain(retainedIndexName.capture(), eq(indexSet));
        assertThat(retainedIndexName.getValue()).containsExactly("test_1");
    }

    @Test
    public void timeBasedNoDates() {
        when(indices.indexClosingDate("test_1")).thenReturn(Optional.empty());
        when(indices.indexCreationDate("test_1")).thenReturn(Optional.empty());

        underTest.retain(indexSet, getIndexLifetimeConfig(14, 16), action, "action");

        verify(action, times(1)).retain(retainedIndexName.capture(), eq(indexSet));
        assertThat(retainedIndexName.getValue()).containsExactly("test_1");
    }

}
