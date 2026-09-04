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
package org.graylog2.indexer.messages;

import com.github.rholder.retry.WaitStrategy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

public class RetryWaitTest {
    @Test
    void secondsBasedRetryWaitsForSecondsStartingWith1() {
        WaitStrategy waitStrategy = new RetryWait(500).waitStrategy;
        assertAll(
                () -> assertThat(waitStrategy.computeSleepTime(new GenericRetryAttempt(1))).isEqualTo(1000),
                () -> assertThat(waitStrategy.computeSleepTime(new GenericRetryAttempt(2))).isEqualTo(2000),
                () -> assertThat(waitStrategy.computeSleepTime(new GenericRetryAttempt(3))).isEqualTo(4000),
                () -> assertThat(waitStrategy.computeSleepTime(new GenericRetryAttempt(4))).isEqualTo(8000),
                () -> assertThat(waitStrategy.computeSleepTime(new GenericRetryAttempt(5))).isEqualTo(16000)
        );
    }
}
