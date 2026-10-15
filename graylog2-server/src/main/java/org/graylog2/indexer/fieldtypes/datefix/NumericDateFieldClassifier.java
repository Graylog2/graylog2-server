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

import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Set;

/**
 * Decides whether a numerically mapped field holds what used to be a date.
 * <p>
 * Graylog 7.1 serialized date values as numbers before the object mapper fix, so an index written back then mapped
 * such a field numerically. The shape of the number says which Java type produced it:
 * <ul>
 *     <li>Joda {@code DateTime} and {@code java.util.Date} - the pipeline functions {@code parse_date},
 *     {@code flex_parse_date}, {@code now}, {@code to_date} and {@code parse_unix_milliseconds}, and the
 *     {@code DATE} / {@code FLEXDATE} extractor converters - produced epoch <em>milliseconds</em>, which dynamic
 *     mapping turns into {@code long}.</li>
 *     <li>{@code java.time} values, most notably the {@code Instant} a Grok {@code ;date;} conversion returns,
 *     produced fractional epoch <em>seconds</em>, which dynamic mapping turns into {@code float}.</li>
 * </ul>
 * This class is deliberately free of any lookups so that the decision can be tested on its own.
 */
public class NumericDateFieldClassifier {

    /**
     * Only the two types dynamic mapping produces are listed, because only those can have been created by the
     * faulty serialization. Any other numeric type comes from a template, a custom field mapping or a profile,
     * which all mean somebody chose it on purpose.
     */
    private static final Set<String> INTEGRAL_TYPES = Set.of("long");
    private static final Set<String> FRACTIONAL_TYPES = Set.of("float");

    /**
     * Epoch milliseconds from 2001-09-09 to 2033-05-18. Values outside this window are not plausible message
     * timestamps for an index that an affected version wrote.
     */
    private static final double MIN_EPOCH_MILLIS = 1_000_000_000_000d;
    private static final double MAX_EPOCH_MILLIS = 2_000_000_000_000d;

    private static final double MIN_EPOCH_SECONDS = MIN_EPOCH_MILLIS / 1000d;
    private static final double MAX_EPOCH_SECONDS = MAX_EPOCH_MILLIS / 1000d;

    /**
     * How far outside the period an index covers the implied timestamps may fall and still count as a date. A date
     * field tracks the messages it was written with, give or take: an event time may lag the message that reported
     * it. A counter that merely happens to land in the epoch window almost never agrees with that period.
     */
    private static final long DATA_PERIOD_TOLERANCE_MILLIS = 30L * 24 * 60 * 60 * 1000;

    public enum Verdict {
        /** The values are epoch milliseconds, so the field was a Joda {@code DateTime} or a {@code java.util.Date}. */
        EPOCH_MILLIS,
        /** The values are fractional epoch seconds, so the field was a {@code java.time} value. */
        EPOCH_SECONDS,
        /** Nothing suggests this field ever held a date. */
        NOT_A_DATE
    }

    /**
     * Classifies a field from its mapped type and the range of values it actually holds.
     *
     * @param physicalType the mapped type, as recorded in {@code index_field_types}
     * @param documentCount how many documents of the index have a value for the field
     * @param min           the smallest value in the index
     * @param max           the largest value in the index
     * @param indexRange    the period the index's documents cover, or {@code null} when it is not known. When
     *                      given, the implied timestamps have to fall near it.
     */
    public Verdict classify(String physicalType,
                            long documentCount,
                            double min,
                            double max,
                            @Nullable IndexPeriod indexRange) {
        if (physicalType == null || documentCount <= 0 || !Double.isFinite(min) || !Double.isFinite(max)) {
            // Without values there is nothing to go on. A field that was mapped but never written cannot be judged.
            return Verdict.NOT_A_DATE;
        }

        final String type = physicalType.toLowerCase(Locale.ROOT);

        if (FRACTIONAL_TYPES.contains(type)
                && within(min, max, MIN_EPOCH_SECONDS, MAX_EPOCH_SECONDS)
                && nearIndexRange(min * 1000d, max * 1000d, indexRange)) {
            return Verdict.EPOCH_SECONDS;
        }

        if (INTEGRAL_TYPES.contains(type)
                && within(min, max, MIN_EPOCH_MILLIS, MAX_EPOCH_MILLIS)
                && nearIndexRange(min, max, indexRange)) {
            return Verdict.EPOCH_MILLIS;
        }

        return Verdict.NOT_A_DATE;
    }

    private static boolean within(double min, double max, double lowerBound, double upperBound) {
        return min >= lowerBound && max <= upperBound;
    }

    private static boolean nearIndexRange(double minMillis, double maxMillis, @Nullable IndexPeriod indexRange) {
        if (indexRange == null) {
            return true;
        }
        return maxMillis >= indexRange.beginMillis() - DATA_PERIOD_TOLERANCE_MILLIS
                && minMillis <= indexRange.endMillis() + DATA_PERIOD_TOLERANCE_MILLIS;
    }

    /**
     * The period an index covers, taken from the timestamps of the documents it holds.
     */
    public record IndexPeriod(long beginMillis, long endMillis) {
        public static IndexPeriod of(DateTime begin, DateTime end) {
            return new IndexPeriod(begin.getMillis(), end.getMillis());
        }
    }
}
