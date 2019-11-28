package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.Optional;

@AutoValue
@JsonAutoDetect
public abstract class OffsetRange extends TimeRange {
    static final String OFFSET = "offset";

    @JsonProperty
    @Override
    abstract String type();

    @JsonProperty
    public abstract String source();

    @JsonProperty
    public abstract Optional<String> id();

    @JsonProperty
    public abstract String offset();

    static OffsetRange ofSearchTypeId(String searchTypeId) {
        return new AutoValue_OffsetRange(OFFSET, "search_type", Optional.of(searchTypeId), "1i");
    }
}
