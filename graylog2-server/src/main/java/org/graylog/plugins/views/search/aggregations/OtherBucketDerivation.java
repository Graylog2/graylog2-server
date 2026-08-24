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

import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Average;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog.plugins.views.search.searchtypes.pivot.series.StdDev;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Sum;
import org.graylog.plugins.views.search.searchtypes.pivot.series.SumOfSquares;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Variance;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * Derives series values for the synthetic {@code (Other)} bucket, which collects all terms falling outside a
 * {@link org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values} grouping's limit.
 * <p>
 * The search backends cannot aggregate over the tail directly, so its values are reconstructed as
 * {@code parent − Σ(shown buckets)}. Only decomposable series can be reconstructed this way; everything else
 * returns {@link Optional#empty()} and is omitted from the result rather than reported as zero.
 * <p>
 * This class is deliberately storage-agnostic: it operates on numbers the adapters have already extracted, so
 * all three search backends share one implementation and one set of tests.
 */
public final class OtherBucketDerivation {

    private OtherBucketDerivation() {
    }

    /**
     * The components of an {@code extended_stats} aggregation needed to reconstruct mean-based series.
     */
    public record Stats(long count, double sum, double sumOfSquares) {
        public static final Stats EMPTY = new Stats(0L, 0.0d, 0.0d);

        public Stats minus(Stats other) {
            return new Stats(count - other.count(), sum - other.sum(), sumOfSquares - other.sumOfSquares());
        }
    }

    /**
     * Everything needed to derive one series at one bucket level.
     *
     * @param otherDocCount the terms aggregation's {@code sum_other_doc_count}, combined across the empty-value split
     * @param parentValue   the series value on the enclosing {@code filters} bucket, or {@code null} if absent
     * @param shownValues   the series value of every terms bucket that made the limit
     * @param parentStats   the companion {@code extended_stats} on the enclosing bucket, or {@code null}
     * @param shownStats    the companion {@code extended_stats} of every terms bucket that made the limit
     */
    public record TailInput(long otherDocCount,
                             @Nullable Double parentValue,
                             List<Double> shownValues,
                             @Nullable Stats parentStats,
                             List<Stats> shownStats) {
    }

    /**
     * Whether a tail value can be reconstructed for this series at all. Series returning {@code false} are
     * omitted from the {@code (Other)} bucket entirely.
     */
    public static boolean isDerivable(SeriesSpec spec) {
        return spec instanceof Count
                || spec instanceof Sum
                || spec instanceof SumOfSquares
                || spec instanceof Average
                || spec instanceof Variance
                || spec instanceof StdDev;
    }

    /**
     * Whether this series needs a companion {@code extended_stats} aggregation to be derivable. Used at query
     * generation time so companions are only added when something actually consumes them.
     */
    public static boolean requiresStats(SeriesSpec spec) {
        return spec instanceof Average || spec instanceof Variance || spec instanceof StdDev || spec instanceof SumOfSquares;
    }

    /**
     * Whether this series has a per-bucket aggregation to read at all. Field-less {@code count()} does not:
     * the backends rely on each bucket's own {@code doc_count}, so no aggregation is emitted for it and the
     * tail comes straight from {@code sum_other_doc_count}. Adapters must consult this before treating a
     * missing value as a reason to bail.
     */
    public static boolean requiresSeriesValue(SeriesSpec spec) {
        return !(spec instanceof Count count && count.field().isEmpty());
    }

    public static Optional<Object> derive(SeriesSpec spec, TailInput input) {
        if (spec instanceof Count count) {
            return count.field().isPresent()
                    ? subtract(input).map(value -> (Object) Math.max(0L, Math.round(value)))
                    : Optional.of((Object) input.otherDocCount());
        }
        if (spec instanceof Sum) {
            return subtract(input).map(Object.class::cast);
        }
        if (spec instanceof SumOfSquares) {
            return tailStats(input).map(Stats::sumOfSquares).map(value -> Math.max(0.0d, value)).map(Object.class::cast);
        }
        if (spec instanceof Average) {
            return tailStats(input).flatMap(OtherBucketDerivation::mean).map(Object.class::cast);
        }
        if (spec instanceof Variance) {
            return tailStats(input).flatMap(OtherBucketDerivation::variance).map(Object.class::cast);
        }
        if (spec instanceof StdDev) {
            return tailStats(input).flatMap(OtherBucketDerivation::variance).map(Math::sqrt).map(Object.class::cast);
        }
        return Optional.empty();
    }

    /**
     * Derives a field-less {@code count()} within a single column of the {@code (Other)} row.
     * {@code sum_other_doc_count} describes the row grouping's own tail, not the tail within one column, so it
     * cannot be used here; the doc count of the tail is reconstructed as {@code parentDocCount - Σ(shownDocCounts)}
     * instead.
     */
    public static Optional<Object> deriveFromDocCounts(SeriesSpec spec, long parentDocCount, List<Long> shownDocCounts) {
        if (spec instanceof Count count && count.field().isEmpty()) {
            final long tail = parentDocCount - shownDocCounts.stream().mapToLong(Long::longValue).sum();
            return tail > 0 ? Optional.of(tail) : Optional.empty();
        }
        return Optional.empty();
    }

    private static Optional<Double> subtract(TailInput input) {
        if (input.parentValue() == null) {
            return Optional.empty();
        }
        final double shown = input.shownValues().stream().mapToDouble(Double::doubleValue).sum();
        return Optional.of(input.parentValue() - shown);
    }

    private static Optional<Stats> tailStats(TailInput input) {
        if (input.parentStats() == null) {
            return Optional.empty();
        }
        Stats tail = input.parentStats();
        for (final Stats shown : input.shownStats()) {
            tail = tail.minus(shown);
        }
        return tail.count() > 0 ? Optional.of(tail) : Optional.empty();
    }

    private static Optional<Double> mean(Stats stats) {
        return stats.count() > 0 ? Optional.of(stats.sum() / stats.count()) : Optional.empty();
    }

    private static Optional<Double> variance(Stats stats) {
        return mean(stats).map(mean -> Math.max(0.0d, (stats.sumOfSquares() / stats.count()) - (mean * mean)));
    }
}
