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

import static org.assertj.core.api.Assertions.assertThat;

class InputMessageSizeDistributorTest {

    @Test
    void distributeProportionally() {
        final long[] sizes = InputMessageSizeDistributor.distribute(900, new long[]{400, 200, 100});

        assertThat(sizes).hasSize(3);
        assertThat(sizes[0]).isGreaterThan(sizes[1]);
        assertThat(sizes[1]).isGreaterThan(sizes[2]);
        assertThat(sizes[0] + sizes[1] + sizes[2]).isEqualTo(900);
    }

    @Test
    void distributeSingleItem() {
        final long[] sizes = InputMessageSizeDistributor.distribute(500, new long[]{42});

        assertThat(sizes).containsExactly(500);
    }

    @Test
    void distributeEmptyArray() {
        final long[] sizes = InputMessageSizeDistributor.distribute(500, new long[]{});

        assertThat(sizes).isEmpty();
    }

    @Test
    void lastItemGetsRemainder() {
        final long[] sizes = InputMessageSizeDistributor.distribute(100, new long[]{1, 1, 1});

        assertThat(sizes[0]).isEqualTo(33);
        assertThat(sizes[1]).isEqualTo(33);
        assertThat(sizes[2]).isEqualTo(34);
    }

    @Test
    void distributeWithEqualWeights() {
        final long[] sizes = InputMessageSizeDistributor.distribute(900, new long[]{100, 100, 100});

        assertThat(sizes[0]).isEqualTo(300);
        assertThat(sizes[1]).isEqualTo(300);
        assertThat(sizes[2]).isEqualTo(300);
    }

    @Test
    void distributeWithTotalSizeLessThenWeightSum() {
        final long[] sizes = InputMessageSizeDistributor.distribute(1, new long[]{2, 1});

        assertThat(sizes[0]).isZero();
        assertThat(sizes[1]).isEqualTo(1);
    }
}
