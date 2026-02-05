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
package org.graylog2.shared.metrics;

import com.codahale.metrics.Clock;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SlidingWindowCounterTest {

    @Test
    void countsOnlyWithinWindow() {
        final TestClock clock = new TestClock();
        final SlidingWindowCounter counter = new SlidingWindowCounter(Duration.ofMinutes(2), clock);

        clock.setTime(0L);
        counter.inc(3);
        assertThat(counter.getCount()).isEqualTo(3L);

        clock.setTime(Duration.ofMinutes(1).toMillis());
        assertThat(counter.getCount()).isEqualTo(3L);

        clock.setTime(Duration.ofMinutes(2).toMillis() + 59_999L);
        assertThat(counter.getCount()).isEqualTo(3L);

        clock.setTime(Duration.ofMinutes(3).toMillis());
        assertThat(counter.getCount()).isEqualTo(0L);
    }

    @Test
    void ignoresNonPositiveDeltas() {
        final TestClock clock = new TestClock();
        final SlidingWindowCounter counter = new SlidingWindowCounter(Duration.ofSeconds(1), clock);

        clock.setTime(0L);
        counter.inc(1);
        counter.inc(0);
        counter.inc(-2);

        assertThat(counter.getCount()).isEqualTo(1L);
    }

    private static class TestClock extends Clock {
        private long time;

        @Override
        public long getTick() {
            return 0;
        }

        @Override
        public long getTime() {
            return time;
        }

        void setTime(long time) {
            this.time = time;
        }
    }
}
