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
package org.graylog2.rest.resources.tools;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TimeLimitedCharSequenceTest {

    @Test
    public void charAtReturnsCorrectCharacterBeforeDeadline() {
        final TimeLimitedCharSequence seq = TimeLimitedCharSequence.withTimeout("hello", 5_000);
        assertThat(seq.charAt(0)).isEqualTo('h');
        assertThat(seq.charAt(4)).isEqualTo('o');
    }

    @Test
    public void charAtThrowsTimeoutExceptionWhenDeadlineIsAlreadyExpired() {
        // timeout=-1 places the deadline 1ms in the past
        final TimeLimitedCharSequence seq = TimeLimitedCharSequence.withTimeout("hello", -1);
        assertThatThrownBy(() -> seq.charAt(0))
                .isInstanceOf(TimeLimitedCharSequence.TimeoutException.class);
    }

    @Test
    public void charAtThrowsTimeoutExceptionOnSubsequentCallAfterDeadlinePasses() throws InterruptedException {
        final TimeLimitedCharSequence seq = TimeLimitedCharSequence.withTimeout("hello", 20);
        assertThat(seq.charAt(0)).isEqualTo('h');
        Thread.sleep(30);
        assertThatThrownBy(() -> seq.charAt(1))
                .isInstanceOf(TimeLimitedCharSequence.TimeoutException.class);
    }

    @Test
    public void subSequenceCharAtThrowsTimeoutExceptionWhenDeadlineIsExpired() {
        final TimeLimitedCharSequence seq = TimeLimitedCharSequence.withTimeout("hello", -1);
        final CharSequence sub = seq.subSequence(1, 4);
        assertThatThrownBy(() -> sub.charAt(0))
                .isInstanceOf(TimeLimitedCharSequence.TimeoutException.class);
    }

    @Test
    public void subSequenceSharesDeadlineWithParent() throws InterruptedException {
        final TimeLimitedCharSequence seq = TimeLimitedCharSequence.withTimeout("hello", 20);
        final CharSequence sub = seq.subSequence(1, 4);
        assertThat(sub.charAt(0)).isEqualTo('e');
        Thread.sleep(30);
        assertThatThrownBy(() -> sub.charAt(1))
                .isInstanceOf(TimeLimitedCharSequence.TimeoutException.class);
    }
}
