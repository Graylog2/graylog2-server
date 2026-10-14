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
package org.graylog2.indexer.fieldtypes.datefix;

import org.graylog2.indexer.fieldtypes.datefix.NumericDateFieldClassifier.IndexPeriod;
import org.graylog2.indexer.fieldtypes.datefix.NumericDateFieldClassifier.Verdict;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NumericDateFieldClassifierTest {

    /** 2025-09-22T08:11:12Z, the value the issue uses as its example. */
    private static final double EPOCH_MILLIS = 1_758_528_672_000d;
    private static final double EPOCH_SECONDS = 1_758_528_672d;

    private static final IndexPeriod MATCHING_PERIOD =
            new IndexPeriod((long) EPOCH_MILLIS - 3_600_000, (long) EPOCH_MILLIS + 3_600_000);

    private final NumericDateFieldClassifier classifier = new NumericDateFieldClassifier();

    @Test
    void recognisesEpochMillisInALongField() {
        assertThat(classifier.classify("long", 10, EPOCH_MILLIS, EPOCH_MILLIS, MATCHING_PERIOD))
                .isEqualTo(Verdict.EPOCH_MILLIS);
    }

    @Test
    void recognisesFractionalEpochSecondsInAFloatField() {
        // what a grok ;date; conversion produced, because java.time values serialize as fractional seconds
        assertThat(classifier.classify("float", 10, EPOCH_SECONDS, EPOCH_SECONDS, MATCHING_PERIOD))
                .isEqualTo(Verdict.EPOCH_SECONDS);
    }

    @Test
    void recognisesTheOtherNumericTypesOfEachShape() {
        assertThat(classifier.classify("unsigned_long", 1, EPOCH_MILLIS, EPOCH_MILLIS, MATCHING_PERIOD))
                .isEqualTo(Verdict.EPOCH_MILLIS);
        assertThat(classifier.classify("double", 1, EPOCH_SECONDS, EPOCH_SECONDS, MATCHING_PERIOD))
                .isEqualTo(Verdict.EPOCH_SECONDS);
        assertThat(classifier.classify("scaled_float", 1, EPOCH_SECONDS, EPOCH_SECONDS, MATCHING_PERIOD))
                .isEqualTo(Verdict.EPOCH_SECONDS);
    }

    @Test
    void doesNotConfuseTheTwoShapes() {
        // epoch millis in a float field would be seconds in the year 57000, and seconds in a long field would be 1970
        assertThat(classifier.classify("float", 10, EPOCH_MILLIS, EPOCH_MILLIS, null)).isEqualTo(Verdict.NOT_A_DATE);
        assertThat(classifier.classify("long", 10, EPOCH_SECONDS, EPOCH_SECONDS, null)).isEqualTo(Verdict.NOT_A_DATE);
    }

    @Test
    void ignoresOrdinaryNumbers() {
        assertThat(classifier.classify("long", 10, 0, 512, MATCHING_PERIOD)).isEqualTo(Verdict.NOT_A_DATE);
        assertThat(classifier.classify("float", 10, 0.1, 99.9, MATCHING_PERIOD)).isEqualTo(Verdict.NOT_A_DATE);
    }

    @Test
    void ignoresNonNumericTypes() {
        assertThat(classifier.classify("date", 10, EPOCH_MILLIS, EPOCH_MILLIS, MATCHING_PERIOD))
                .isEqualTo(Verdict.NOT_A_DATE);
        assertThat(classifier.classify("keyword", 10, EPOCH_MILLIS, EPOCH_MILLIS, MATCHING_PERIOD))
                .isEqualTo(Verdict.NOT_A_DATE);
        assertThat(classifier.classify(null, 10, EPOCH_MILLIS, EPOCH_MILLIS, MATCHING_PERIOD))
                .isEqualTo(Verdict.NOT_A_DATE);
    }

    @Test
    void ignoresAFieldWithoutValues() {
        // a field that was mapped but never written cannot be judged, and must not trigger a rotation
        assertThat(classifier.classify("long", 0, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, null))
                .isEqualTo(Verdict.NOT_A_DATE);
        assertThat(classifier.classify("long", 0, EPOCH_MILLIS, EPOCH_MILLIS, null)).isEqualTo(Verdict.NOT_A_DATE);
    }

    @Test
    void ignoresValuesOutsideThePlausibleEpochWindow() {
        assertThat(classifier.classify("long", 10, 1d, 1d, null)).isEqualTo(Verdict.NOT_A_DATE);
        assertThat(classifier.classify("long", 10, 9_000_000_000_000d, 9_000_000_000_000d, null))
                .isEqualTo(Verdict.NOT_A_DATE);
    }

    @Test
    void ignoresCountersThatOnlyLookLikeTimestamps() {
        // ~1.75 TB in bytes lands inside the epoch window, but nowhere near when this index was written
        final IndexPeriod lastYear = IndexPeriod.of(
                new DateTime(2020, 1, 1, 0, 0, DateTimeZone.UTC),
                new DateTime(2020, 1, 2, 0, 0, DateTimeZone.UTC));

        assertThat(classifier.classify("long", 10, EPOCH_MILLIS, EPOCH_MILLIS, lastYear))
                .isEqualTo(Verdict.NOT_A_DATE);
    }

    @Test
    void acceptsTimestampsNearButOutsideTheIndexRange() {
        // an index holds messages whose timestamps may drift somewhat beyond the range it was calculated for
        final IndexPeriod shortlyBefore = new IndexPeriod(
                (long) EPOCH_MILLIS - 30L * 24 * 60 * 60 * 1000,
                (long) EPOCH_MILLIS - 29L * 24 * 60 * 60 * 1000);

        assertThat(classifier.classify("long", 10, EPOCH_MILLIS, EPOCH_MILLIS, shortlyBefore))
                .isEqualTo(Verdict.EPOCH_MILLIS);
    }

    @Test
    void worksWithoutAnIndexRange() {
        assertThat(classifier.classify("long", 10, EPOCH_MILLIS, EPOCH_MILLIS, null)).isEqualTo(Verdict.EPOCH_MILLIS);
        assertThat(classifier.classify("float", 10, EPOCH_SECONDS, EPOCH_SECONDS, null))
                .isEqualTo(Verdict.EPOCH_SECONDS);
    }

    @Test
    void requiresTheWholeValueRangeToLookLikeATimestamp() {
        // a field holding both timestamps and small numbers is something else
        assertThat(classifier.classify("long", 10, 0, EPOCH_MILLIS, null)).isEqualTo(Verdict.NOT_A_DATE);
    }
}
