package org.graylog.plugins.enterprise.search.searchtypes;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.enterprise.search.Filter;
import org.graylog.plugins.enterprise.search.SearchType;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;

@AutoValue
@JsonTypeName(DateHistogram.NAME)
@JsonDeserialize(builder = AutoValue_DateHistogram.Builder.class)
public abstract class DateHistogram implements SearchType {
    public static final String NAME = "date_histogram";

    @Override
    public abstract String type();

    @Override
    @Nullable
    @JsonProperty
    public abstract String id();

    @Nullable
    @Override
    public abstract Filter filter();

    @JsonProperty
    public abstract Searches.DateHistogramInterval interval();

    @Override
    public SearchType applyExecutionContext(ObjectMapper objectMapper, JsonNode state) {
        if (state.hasNonNull("interval")) {
            final String interval = state.path("interval").asText();
            final Builder builder = toBuilder()
                    .interval(Searches.DateHistogramInterval.valueOf(interval.toUpperCase(Locale.ENGLISH)));
            return builder.build();
        }
        return this;
    }

    abstract Builder toBuilder();

    public static Builder builder() {
        return new AutoValue_DateHistogram.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty
        public abstract Builder type(String type);

        @JsonProperty
        public abstract Builder id(@Nullable String id);

        @JsonProperty
        public abstract Builder filter(@Nullable Filter filter);

        @JsonProperty
        public abstract Builder interval(Searches.DateHistogramInterval interval);

        public abstract DateHistogram build();
    }

    @AutoValue
    public abstract static class Result implements SearchType.Result {

        @Override
        @JsonProperty
        public abstract String id();

        @Override
        @JsonProperty
        public String type() {
            return NAME;
        }

        @JsonProperty
        public abstract Map<Long, Long> results();

        @JsonProperty
        public abstract AbsoluteRange timerange();

        public static DateHistogram.Result.Builder builder() {
            return new AutoValue_DateHistogram_Result.Builder();
        }

        public static DateHistogram.Result.Builder result(String searchTypeId) {
            return builder().id(searchTypeId);
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract DateHistogram.Result.Builder id(String id);

            public abstract Builder results(Map<Long, Long> results);

            public abstract Builder timerange(AbsoluteRange timerange);

            public abstract DateHistogram.Result build();
        }
    }
}
