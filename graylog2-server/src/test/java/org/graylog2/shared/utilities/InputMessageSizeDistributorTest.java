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
package org.graylog2.shared.utilities;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InputMessageSizeDistributorTest {

    @Test
    void distributeProportionally() {
        final List<Long> sizes = InputMessageSizeDistributor.distribute(900, List.of(400L, 200L, 100L));

        assertThat(sizes).hasSize(3);
        assertThat(sizes.get(0)).isGreaterThan(sizes.get(1));
        assertThat(sizes.get(1)).isGreaterThan(sizes.get(2));
        assertThat(sizes.get(0) + sizes.get(1) + sizes.get(2)).isEqualTo(900);
    }

    @Test
    void distributeEvenlyWhenAllWeightsAreZero() {
        final List<Long> sizes = InputMessageSizeDistributor.distribute(900, List.of(0L, 0L, 0L));

        assertThat(sizes.get(0)).isEqualTo(300);
        assertThat(sizes.get(1)).isEqualTo(300);
        assertThat(sizes.get(2)).isEqualTo(300);
    }

    @Test
    void tryDistributeEvenlyWhenAllWeightsAreZero() {
        final List<Long> sizes = InputMessageSizeDistributor.distribute(9, List.of(0L, 0L));

        assertThat(sizes.get(0)).isEqualTo(4);
        assertThat(sizes.get(1)).isEqualTo(5);
    }

    @Test
    void distributeSingleItem() {
        final List<Long> sizes = InputMessageSizeDistributor.distribute(500, List.of(42L));

        assertThat(sizes).containsExactly(500L);
    }

    @Test
    void distributeEmptyList() {
        final List<Long> sizes = InputMessageSizeDistributor.distribute(500, List.of());

        assertThat(sizes).isEmpty();
    }

    @Test
    void lastItemGetsRemainder() {
        final List<Long> sizes = InputMessageSizeDistributor.distribute(100, List.of(1L, 1L, 1L));

        assertThat(sizes.get(0)).isEqualTo(33);
        assertThat(sizes.get(1)).isEqualTo(33);
        assertThat(sizes.get(2)).isEqualTo(34);
    }

    @Test
    void distributeWithEqualWeights() {
        final List<Long> sizes = InputMessageSizeDistributor.distribute(900, List.of(100L, 100L, 100L));

        assertThat(sizes.get(0)).isEqualTo(300);
        assertThat(sizes.get(1)).isEqualTo(300);
        assertThat(sizes.get(2)).isEqualTo(300);
    }

    @Test
    void distributeWithTotalSizeLessThenWeightSum() {
        final List<Long> sizes = InputMessageSizeDistributor.distribute(1, List.of(2L, 1L));

        assertThat(sizes.get(0)).isZero();
        assertThat(sizes.get(1)).isEqualTo(1);
    }
}
