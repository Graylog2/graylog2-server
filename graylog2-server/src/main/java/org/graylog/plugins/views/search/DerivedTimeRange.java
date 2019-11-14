package org.graylog.plugins.views.search;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.auto.value.AutoValue;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.KeywordRange;
import org.graylog2.plugin.indexer.searches.timeranges.OffsetRange;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

@AutoValue
@JsonAutoDetect
public abstract class DerivedTimeRange {
    @JsonValue
    abstract TimeRange value();

    public TimeRange effectiveTimeRange(Query query, SearchType searchType) {
        switch (value().type()) {
            case AbsoluteRange.ABSOLUTE:
            case KeywordRange.KEYWORD:
            case RelativeRange.RELATIVE: return value();
            case OffsetRange.OFFSET: return ((OffsetRange) value()).deriveTimeRange(query, searchType);
            default:
                throw new IllegalArgumentException("Invalid time range: " + value());
        }
    }

    @JsonCreator
    public static DerivedTimeRange of(TimeRange timeRange) {
        return new AutoValue_DerivedTimeRange(timeRange);
    }
}
