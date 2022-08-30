package org.graylog2.plugin.indexer.searches.timeranges;

import org.assertj.core.api.Assertions;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

class AbsoluteRangeTest {

    @Test
    void testBuilderWithoutExplicitType() {
        final AbsoluteRange range = AbsoluteRange.builder()
                .from(DateTime.now().minus(1000 * 60 * 50))
                .to(DateTime.now())
                .build();
        Assertions.assertThat(range.type()).isEqualTo(AbsoluteRange.ABSOLUTE);
    }
}
