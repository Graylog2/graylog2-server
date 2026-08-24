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
package org.graylog.plugins.views.search.aggregations;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.graylog.plugins.views.search.aggregations.OtherBucketDerivation.Stats;
import org.graylog.plugins.views.search.aggregations.OtherBucketDerivation.TailInput;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Average;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Cardinality;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Latest;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Max;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Min;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Percentile;
import org.graylog.plugins.views.search.searchtypes.pivot.series.StdDev;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Sum;
import org.graylog.plugins.views.search.searchtypes.pivot.series.SumOfSquares;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Variance;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class OtherBucketDerivationTest {

    private static TailInput plain(long otherDocCount, Double parentValue, Double... shown) {
        return new TailInput(otherDocCount, parentValue, List.of(shown), null, List.of());
    }

    private static TailInput withStats(Stats parent, Stats... shown) {
        return new TailInput(0L, null, List.of(), parent, List.of(shown));
    }

    @Test
    void countWithoutFieldUsesTailDocCount() {
        final var result = OtherBucketDerivation.derive(Count.builder().build(), plain(42L, null));

        assertThat(result).contains(42L);
    }

    @Test
    void countWithFieldSubtractsValueCounts() {
        final var spec = Count.builder().field("age").build();

        final var result = OtherBucketDerivation.derive(spec, plain(0L, 100.0d, 30.0d, 20.0d));

        assertThat(result).contains(50L);
    }

    @Test
    void sumSubtractsShownFromParent() {
        final var result = OtherBucketDerivation.derive(Sum.builder().field("age").build(),
                plain(0L, 100.0d, 30.0d, 20.0d));

        assertThat(result).contains(50.0d);
    }

    @Test
    void sumMayLegitimatelyBeNegative() {
        final var result = OtherBucketDerivation.derive(Sum.builder().field("delta").build(),
                plain(0L, 10.0d, 30.0d));

        assertThat(result).contains(-20.0d);
    }

    @Test
    void averageIsDerivedFromStats() {
        // parent: 5 values summing to 100. shown: 3 values summing to 40. tail: 2 values summing to 60 -> avg 30.
        final var result = OtherBucketDerivation.derive(Average.builder().field("age").build(),
                withStats(new Stats(5L, 100.0d, 0.0d), new Stats(3L, 40.0d, 0.0d)));

        assertThat(result).get().asInstanceOf(InstanceOfAssertFactories.DOUBLE)
                .isCloseTo(30.0d, within(1e-9));
    }

    @Test
    void averageWithWholeNumberRoundsTheResult() {
        // tail: 2 values summing to 61 -> avg 30.5, which whole_number rounds to 31.
        final var result = OtherBucketDerivation.derive(Average.builder().field("age").wholeNumber(true).build(),
                withStats(new Stats(5L, 101.0d, 0.0d), new Stats(3L, 40.0d, 0.0d)));

        assertThat(result).get().asInstanceOf(InstanceOfAssertFactories.DOUBLE)
                .isCloseTo(31.0d, within(1e-9));
    }

    @Test
    void averageWithoutWholeNumberKeepsTheFraction() {
        final var result = OtherBucketDerivation.derive(Average.builder().field("age").wholeNumber(false).build(),
                withStats(new Stats(5L, 101.0d, 0.0d), new Stats(3L, 40.0d, 0.0d)));

        assertThat(result).get().asInstanceOf(InstanceOfAssertFactories.DOUBLE)
                .isCloseTo(30.5d, within(1e-9));
    }

    @Test
    void averageIsOmittedWhenTailIsEmpty() {
        final var result = OtherBucketDerivation.derive(Average.builder().field("age").build(),
                withStats(new Stats(3L, 40.0d, 0.0d), new Stats(3L, 40.0d, 0.0d)));

        assertThat(result).isEmpty();
    }

    @Test
    void varianceIsDerivedFromStats() {
        // tail: n=2, sum=10 (values 3 and 7), sumOfSquares=58. mean=5, variance=58/2 - 25 = 4.
        final var result = OtherBucketDerivation.derive(Variance.builder().field("age").build(),
                withStats(new Stats(3L, 20.0d, 158.0d), new Stats(1L, 10.0d, 100.0d)));

        assertThat(result).get().asInstanceOf(InstanceOfAssertFactories.DOUBLE)
                .isCloseTo(4.0d, within(1e-9));
    }

    @Test
    void stdDevIsTheSquareRootOfVariance() {
        final var result = OtherBucketDerivation.derive(StdDev.builder().field("age").build(),
                withStats(new Stats(3L, 20.0d, 158.0d), new Stats(1L, 10.0d, 100.0d)));

        assertThat(result).get().asInstanceOf(InstanceOfAssertFactories.DOUBLE)
                .isCloseTo(2.0d, within(1e-9));
    }

    @Test
    void sumOfSquaresIsDerivedFromStats() {
        // tail: sumOfSquares = 158.0 - 100.0 = 58.0.
        final var result = OtherBucketDerivation.derive(SumOfSquares.builder().field("age").build(),
                withStats(new Stats(3L, 20.0d, 158.0d), new Stats(1L, 10.0d, 100.0d)));

        assertThat(result).contains(58.0d);
    }

    @Test
    void sumOfSquaresClampsNegativeDriftToZero() {
        // parent's sumOfSquares is one ulp below 9.0, shown's is exactly 9.0: the subtraction lands just
        // under zero from floating-point drift, not from a real negative sum of squares. Only the clamp
        // keeps this at 0.
        final var result = OtherBucketDerivation.derive(SumOfSquares.builder().field("age").build(),
                withStats(new Stats(2L, 6.0d, 8.999999999999998d), new Stats(1L, 3.0d, 9.0d)));

        assertThat(result).get().asInstanceOf(InstanceOfAssertFactories.DOUBLE)
                .isEqualTo(0.0d);
    }

    @Test
    void varianceClampsNegativeDriftToZero() {
        // A single-value tail has variance exactly 0; float drift can push sumOfSquares just under mean^2,
        // making the raw computation negative. Only the clamp keeps this at 0.
        final var result = OtherBucketDerivation.derive(Variance.builder().field("age").build(),
                withStats(new Stats(1L, 3.0d, 8.999999999999998d), Stats.EMPTY));

        assertThat(result).get().asInstanceOf(InstanceOfAssertFactories.DOUBLE)
                .isEqualTo(0.0d);
    }

    @Test
    void nonDerivableSeriesReturnEmpty() {
        final var input = plain(10L, 100.0d, 20.0d);

        assertThat(OtherBucketDerivation.derive(Min.builder().field("age").build(), input)).isEmpty();
        assertThat(OtherBucketDerivation.derive(Max.builder().field("age").build(), input)).isEmpty();
        assertThat(OtherBucketDerivation.derive(Cardinality.builder().field("age").build(), input)).isEmpty();
        assertThat(OtherBucketDerivation.derive(Percentile.builder().field("age").percentile(95.0d).build(), input)).isEmpty();
        assertThat(OtherBucketDerivation.derive(Latest.builder().field("age").build(), input)).isEmpty();
    }

    @Test
    void isDerivableReportsSupportedSeries() {
        assertThat(OtherBucketDerivation.isDerivable(Count.builder().build())).isTrue();
        assertThat(OtherBucketDerivation.isDerivable(Sum.builder().field("age").build())).isTrue();
        assertThat(OtherBucketDerivation.isDerivable(SumOfSquares.builder().field("age").build())).isTrue();
        assertThat(OtherBucketDerivation.isDerivable(Average.builder().field("age").build())).isTrue();
        assertThat(OtherBucketDerivation.isDerivable(Variance.builder().field("age").build())).isTrue();
        assertThat(OtherBucketDerivation.isDerivable(StdDev.builder().field("age").build())).isTrue();

        assertThat(OtherBucketDerivation.isDerivable(Min.builder().field("age").build())).isFalse();
        assertThat(OtherBucketDerivation.isDerivable(Cardinality.builder().field("age").build())).isFalse();
    }

    @Test
    void requiresStatsOnlyForSeriesThatNeedThem() {
        assertThat(OtherBucketDerivation.requiresStats(Average.builder().field("age").build())).isTrue();
        assertThat(OtherBucketDerivation.requiresStats(Variance.builder().field("age").build())).isTrue();
        assertThat(OtherBucketDerivation.requiresStats(StdDev.builder().field("age").build())).isTrue();
        assertThat(OtherBucketDerivation.requiresStats(SumOfSquares.builder().field("age").build())).isTrue();

        assertThat(OtherBucketDerivation.requiresStats(Count.builder().build())).isFalse();
        assertThat(OtherBucketDerivation.requiresStats(Sum.builder().field("age").build())).isFalse();
        assertThat(OtherBucketDerivation.requiresStats(Min.builder().field("age").build())).isFalse();
    }

    @Test
    void requiresSeriesValueIsFalseOnlyForFieldLessCount() {
        assertThat(OtherBucketDerivation.requiresSeriesValue(Count.builder().build())).isFalse();

        assertThat(OtherBucketDerivation.requiresSeriesValue(Count.builder().field("age").build())).isTrue();
        assertThat(OtherBucketDerivation.requiresSeriesValue(Sum.builder().field("age").build())).isTrue();
        assertThat(OtherBucketDerivation.requiresSeriesValue(Average.builder().field("age").build())).isTrue();
    }

    @Test
    void statsMinusSubtractsComponentwise() {
        final Stats result = new Stats(5L, 100.0d, 1000.0d).minus(new Stats(2L, 30.0d, 400.0d));

        assertThat(result).isEqualTo(new Stats(3L, 70.0d, 600.0d));
    }

    @Test
    void missingParentValueOmitsTheSeries() {
        final var result = OtherBucketDerivation.derive(Sum.builder().field("age").build(),
                new TailInput(0L, null, List.of(10.0d), null, List.of()));

        assertThat(result).isEmpty();
    }

    @Test
    void deriveFromDocCountsSubtractsShownFromParent() {
        assertThat(OtherBucketDerivation.deriveFromDocCounts(Count.builder().build(), 10L, List.of(4L, 3L)))
                .contains(3L);
    }

    @Test
    void deriveFromDocCountsOmitsEmptyTail() {
        assertThat(OtherBucketDerivation.deriveFromDocCounts(Count.builder().build(), 7L, List.of(4L, 3L)))
                .isEmpty();
    }

    @Test
    void deriveFromDocCountsIsOnlyForFieldLessCount() {
        assertThat(OtherBucketDerivation.deriveFromDocCounts(Count.builder().field("age").build(), 10L, List.of(4L)))
                .isEmpty();
        assertThat(OtherBucketDerivation.deriveFromDocCounts(Sum.builder().field("age").build(), 10L, List.of(4L)))
                .isEmpty();
    }
}
