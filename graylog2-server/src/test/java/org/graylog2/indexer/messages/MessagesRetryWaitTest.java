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

public class MessagesRetryWaitTest {
    @Test
    void secondsBasedRetryWaitsForSecondsStartingWith1() {
        WaitStrategy waitStrategy = Messages.exponentialWaitSeconds;
        assertAll(
                () -> assertThat(waitStrategy.computeSleepTime(new IndexBlockRetryAttempt(1))).isEqualTo(1000),
                () -> assertThat(waitStrategy.computeSleepTime(new IndexBlockRetryAttempt(2))).isEqualTo(2000),
                () -> assertThat(waitStrategy.computeSleepTime(new IndexBlockRetryAttempt(3))).isEqualTo(4000),
                () -> assertThat(waitStrategy.computeSleepTime(new IndexBlockRetryAttempt(4))).isEqualTo(8000),
                () -> assertThat(waitStrategy.computeSleepTime(new IndexBlockRetryAttempt(5))).isEqualTo(16000)
        );
    }

    // This test was added to document how the retry strategy actually behaves since this is hard to deduct from the code
    @Test
    void millisecondsBasedRetryWaitsForMillisecondsStartingWith2() {
        WaitStrategy waitStrategy = Messages.exponentialWaitMilliseconds;
        assertAll(
                () -> assertThat(waitStrategy.computeSleepTime(new IndexBlockRetryAttempt(1))).isEqualTo(2),
                () -> assertThat(waitStrategy.computeSleepTime(new IndexBlockRetryAttempt(2))).isEqualTo(4),
                () -> assertThat(waitStrategy.computeSleepTime(new IndexBlockRetryAttempt(3))).isEqualTo(8),
                () -> assertThat(waitStrategy.computeSleepTime(new IndexBlockRetryAttempt(4))).isEqualTo(16),
                () -> assertThat(waitStrategy.computeSleepTime(new IndexBlockRetryAttempt(5))).isEqualTo(32)
        );
    }
}
