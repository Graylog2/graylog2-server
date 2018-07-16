package org.graylog2.plugin.indexer.searches.timeranges;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RelativeRangeTest {
    @Test
    public void relativeRangeCreatesExactDuration() throws Exception {
        final long rangeInMillis = 300000L;
        final RelativeRange relativeRange = RelativeRange.create(new Long(rangeInMillis / 1000).intValue());

        assertThat(relativeRange.getTo().getMillis() - relativeRange.getFrom().getMillis()).isEqualTo(rangeInMillis);
    }

    @Test(expected = InvalidRangeParametersException.class)
    public void throwsExceptionForNegativeRange() throws Exception {
        RelativeRange.create(-1);
    }
}