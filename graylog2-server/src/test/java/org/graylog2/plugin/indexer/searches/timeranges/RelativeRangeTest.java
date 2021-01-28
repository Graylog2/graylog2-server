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
package org.graylog2.plugin.indexer.searches.timeranges;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RelativeRangeTest {
    private static final DateTime now = DateTime.parse("2021-01-26T15:59:30.428Z");
    @BeforeEach
    void setUp() {
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());
    }

    @AfterEach
    void tearDown() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    void supportsRangeParameter() throws Exception {
        final RelativeRange range = RelativeRange.Builder.builder().range(300).build();
        assertThat(range.getFrom()).isEqualTo(now.minusMinutes(5));
        assertThat(range.getTo()).isEqualTo(now);
    }

    @Test
    void supportsFromParameter() throws Exception {
        final RelativeRange range = RelativeRange.Builder.builder().from(300).build();
        assertThat(range.getFrom()).isEqualTo(now.minusMinutes(5));
        assertThat(range.getTo()).isEqualTo(now);
    }

    @Test
    void supportsFromAndToParameter() throws Exception {
        final RelativeRange range = RelativeRange.Builder.builder()
                .from(300)
                .to(60)
                .build();
        assertThat(range.getFrom()).isEqualTo(now.minusMinutes(5));
        assertThat(range.getTo()).isEqualTo(now.minusMinutes(1));
    }

    @Test
    void doesNotSupportBothRangeAndFromToParameters() {
        assertThatThrownBy(() -> RelativeRange.Builder.builder()
                .range(600)
                .from(300)
                .to(60)
                .build())
                .isInstanceOf(InvalidRangeParametersException.class)
                .hasMessage("Either `range` OR `from`/`to` must be specified, not both!");
    }

    @Test
    void doesNotSupportOnlyToParameter() {
        assertThatThrownBy(() -> RelativeRange.Builder.builder()
                .to(60)
                .build())
                .isInstanceOf(InvalidRangeParametersException.class)
                .hasMessage("If `to` is specified, `from` must be specified to!");
    }
}
