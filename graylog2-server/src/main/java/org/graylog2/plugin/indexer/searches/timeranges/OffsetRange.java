package org.graylog2.plugin.indexer.searches.timeranges;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchType;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AutoValue
@JsonTypeName(OffsetRange.OFFSET)
@JsonDeserialize(builder = OffsetRange.Builder.class)
public abstract class OffsetRange extends TimeRange {
    public static final String OFFSET = "offset";
    private static final Pattern INTERVAL_OFFSET = Pattern.compile("(\\d+)i");

    @JsonProperty
    @Override
    public abstract String type();

    @JsonProperty
    public abstract String source();

    @JsonProperty
    public abstract String id();

    @JsonProperty
    public abstract String offset();

    @Override
    public DateTime getFrom() {
        throw new IllegalStateException("OffsetRange is not able to return its start point on its own. Please use DerivedTimeRange#effectiveTimeRange instead.");
    }

    @Override
    public DateTime getTo() {
        throw new IllegalStateException("OffsetRange is not able to return its end point on its own. Please use DerivedTimeRange#effectiveTimeRange instead.");
    }

    @Override
    @Deprecated
    // Only called from pre 3.2 dashboarding functionality, which does not support offset time ranges.
    public Map<String, Object> getPersistedConfig() {
        return null;
    }

    private TimeRange timeRangeOfSource(String source, String id, Query query, SearchType searchType) {
        switch (source.toLowerCase(Locale.ROOT)) {
            case "query":
                return query.timerange();
            case "search_type":
                return query.searchTypes().stream()
                        .filter(s -> s.id().equals(id))
                        .findFirst()
                        .map(query::effectiveTimeRange)
                        .orElseThrow(() -> new RuntimeException("Search type " + searchType.id() + " has offset timerange referencing invalid search type: " + id));
            default:
                throw new RuntimeException("Search type " + searchType.id() + " has offset timerange referencing invalid source: " + source);
        }
    }

    private Optional<Integer> parseIntervalOffset(String offset) {
        final Matcher intervalMatcher = INTERVAL_OFFSET.matcher(offset);
        if (intervalMatcher.matches()) {
            try {
                return Optional.of(Integer.parseInt(intervalMatcher.group(1), 10));
            } catch (NumberFormatException e) {
                throw new RuntimeException("Offset time range has invalid interval specification: " + offset, e);
            }
        }
        return Optional.empty();
    }

    private long deltaFromOffset(String offset, TimeRange referenceTimeRange) {
        return parseIntervalOffset(offset)
                .map(intervals -> new Duration(referenceTimeRange.getFrom(), referenceTimeRange.getTo()).getMillis() * intervals)
                .orElseGet(() -> (long) Integer.parseInt(offset, 10) * 1000);
    }

    public TimeRange deriveTimeRange(Query query, SearchType searchType) {
        final TimeRange referenceTimeRange = timeRangeOfSource(source(), id(), query, searchType);
        final long delta = deltaFromOffset(offset(), referenceTimeRange);
        return AbsoluteRange.create(referenceTimeRange.getFrom().minus(delta), referenceTimeRange.getTo().minus(delta));
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty
        public abstract Builder type(String type);
        @JsonProperty
        public abstract Builder source(String source);
        @JsonProperty
        public abstract Builder id(String id);
        @JsonProperty
        public abstract Builder offset(@Nullable String offset);
        public Builder offset(Integer offset) {
            return offset(offset.toString());
        }

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_OffsetRange.Builder()
                    .offset("1i");
        }
        public abstract OffsetRange build();
    }
}
