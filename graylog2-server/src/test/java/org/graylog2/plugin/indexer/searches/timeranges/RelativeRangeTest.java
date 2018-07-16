package org.graylog2.plugin.indexer.searches.timeranges;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.Assert;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RelativeRangeTest {
    @Test
    public void relativeRangeCreatesExactTimestamps() throws Exception {
        DateTimeUtils.setCurrentMillisFixed(1531734983860L);
        final RelativeRange relativeRange = RelativeRange.create(300);

        assertThat(relativeRange.getTo()).isEqualTo(DateTime.parse("2018-07-16T09:56:23.860Z"));
        assertThat(relativeRange.getFrom()).isEqualTo(DateTime.parse("2018-07-16T09:51:23.860Z"));
    }

    @Test
    public void relativeRangeCreatesExactTimestampsForZeroRange() throws Exception {
        DateTimeUtils.setCurrentMillisFixed(1531734983860L);
        final RelativeRange relativeRange = RelativeRange.create(0);

        assertThat(relativeRange.getTo()).isEqualTo(DateTime.parse("2018-07-16T09:56:23.860Z"));
        assertThat(relativeRange.getFrom()).isEqualTo(DateTime.parse("2018-07-16T09:56:23.860Z"));
    }

    @Test
    public void relativeRangeCreatesTimestampsUponCreation() throws Exception {
        DateTimeUtils.setCurrentMillisFixed(1531734983860L);
        final RelativeRange relativeRange = RelativeRange.create(0);

        DateTimeUtils.setCurrentMillisFixed(1531735801041L);

        assertThat(relativeRange.getTo()).isEqualTo(DateTime.parse("2018-07-16T09:56:23.860Z"));
        assertThat(relativeRange.getFrom()).isEqualTo(DateTime.parse("2018-07-16T09:56:23.860Z"));
    }

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

    @Test(expected = InvalidRangeParametersException.class)
    public void builderThrowsExceptionForNegativeRange() throws Exception {
        RelativeRange.builder().range(-1).build();
    }

    @Test
    public void builderThrowsExceptionWhenNoRangeIsSet() throws Exception {
        try {
            RelativeRange.builder().build();
            Assert.fail("No exception thrown, although an IllegalStateException was expected.");
        } catch(IllegalStateException e) {
            assertThat(e).hasMessage("Property \"range\" has not been set");
        }
    }
}