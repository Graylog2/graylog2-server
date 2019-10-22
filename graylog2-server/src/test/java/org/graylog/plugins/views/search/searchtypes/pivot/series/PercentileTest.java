package org.graylog.plugins.views.search.searchtypes.pivot.series;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class PercentileTest {

    @Test
    public void testLiteral() {
        final Percentile percentile1 = Percentile.builder()
                .percentile(25.0)
                .field("cloverfield")
                .id("dead-beef")
                .build();
        assertThat(percentile1.literal()).isEqualTo("percentile(cloverfield,25.0)");

        final Percentile percentile2 = Percentile.builder()
                .percentile(99.0)
                .field("nostromo")
                .id("dead-beef")
                .build();
        assertThat(percentile2.literal()).isEqualTo("percentile(nostromo,99.0)");
    }
}
