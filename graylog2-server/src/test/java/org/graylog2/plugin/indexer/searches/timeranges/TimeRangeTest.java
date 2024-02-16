package org.graylog2.plugin.indexer.searches.timeranges;

import org.graylog.plugins.views.search.timeranges.OffsetRange;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import static org.graylog.testing.jackson.JacksonSubtypesAssertions.assertThatDto;

class TimeRangeTest {
    @Test
    void subtypes() {
        final var now = DateTime.now(DateTimeZone.UTC);

        final var absoluteRange = AbsoluteRange.create(now, now);
        final var relativeRange = RelativeRange.create(500);
        final var keywordRange = KeywordRange.create("yesterday", "UTC");
        final var offsetRange = OffsetRange.Builder.builder()
                .offset(1)
                .source("foo")
                .build();

        assertThatDto(absoluteRange)
                .doesNotSerializeWithDuplicateFields()
                .deserializesWhenGivenSupertype(TimeRange.class);
        assertThatDto(relativeRange)
                .doesNotSerializeWithDuplicateFields()
                .deserializesWhenGivenSupertype(TimeRange.class);
        assertThatDto(keywordRange)
                .doesNotSerializeWithDuplicateFields()
                .deserializesWhenGivenSupertype(TimeRange.class);
        assertThatDto(offsetRange)
                .doesNotSerializeWithDuplicateFields()
                .deserializesWhenGivenSupertype(TimeRange.class);
    }
}
