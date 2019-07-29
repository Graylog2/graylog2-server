package org.graylog.events.processor.aggregation;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Average;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Cardinality;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Max;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Min;
import org.graylog.plugins.views.search.searchtypes.pivot.series.StdDev;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Sum;
import org.graylog.plugins.views.search.searchtypes.pivot.series.SumOfSquares;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Variance;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.function.BiFunction;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;

public enum AggregationFunction {
    @JsonProperty("avg")
    AVG((id, field) -> Average.builder().id(id).field(field).build(), true),

    @JsonProperty("card")
    CARD((id, field) -> Cardinality.builder().id(id).field(field).build(), true),

    @JsonProperty("count")
    COUNT((id, field) -> Count.builder().id(id).field(field).build(), false),

    @JsonProperty("max")
    MAX((id, field) -> Max.builder().id(id).field(field).build(), true),

    @JsonProperty("min")
    MIN((id, field) -> Min.builder().id(id).field(field).build(), true),

    @JsonProperty("stddev")
    STDDEV((id, field) -> StdDev.builder().id(id).field(field).build(), true),

    @JsonProperty("sum")
    SUM((id, field) -> Sum.builder().id(id).field(field).build(), true),

    @JsonProperty("sumofsquares")
    SUMOFSQUARES((id, field) -> SumOfSquares.builder().id(id).field(field).build(), true),

    @JsonProperty("variance")
    VARIANCE((id, field) -> Variance.builder().id(id).field(field).build(), true);

    private final BiFunction<String, String, SeriesSpec> seriesSpecSupplier;
    private final boolean requiresField;

    AggregationFunction(BiFunction<String, String, SeriesSpec> seriesSpecFunction, boolean requiresField) {
        this.requiresField = requiresField;
        this.seriesSpecSupplier = requireNonNull(seriesSpecFunction, "SeriesSpec supplier cannot be null");
    }

    public SeriesSpec toSeriesSpec(String id, @Nullable String field) {
        if (requiresField && isNullOrEmpty(field)) {
            throw new IllegalArgumentException("Function <" + toString().toLowerCase(Locale.US) + "> requires a field");
        }
        return seriesSpecSupplier.apply(id, field);
    }
}
