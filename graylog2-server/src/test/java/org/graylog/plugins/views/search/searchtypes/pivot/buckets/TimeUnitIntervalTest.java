/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.views.search.searchtypes.pivot.buckets;

import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class TimeUnitIntervalTest {
    private TimeUnitInterval.Builder builder() {
        return TimeUnitInterval.Builder.builder();
    }

    @Test
    public void doesNotAllowInvalidTimeUnit() {
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> builder().timeunit("foobar").build())
                .withMessage("Time unit must be {quantity}{unit}, where quantity is a positive number and unit [smhdwM].");
    }

    @Test
    public void doesNotAllowNegativeQuantity() {
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> builder().timeunit("-1s").build())
                .withMessage("Time unit must be {quantity}{unit}, where quantity is a positive number and unit [smhdwM].");
    }

    @Test
    public void doesNotAllowZeroQuantity() {
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> builder().timeunit("0d").build())
                .withMessage("Time unit's value must be a positive number, greater than zero.");
    }

    @Test
    public void doesNotAllowUnknownUnit() {
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> builder().timeunit("1x").build())
                .withMessage("Time unit must be {quantity}{unit}, where quantity is a positive number and unit [smhdwM].");
    }

    @Test
    public void allowsPositiveQuantityAndKnownUnit() throws InvalidRangeParametersException {
        final TimeUnitInterval timeunit = builder().timeunit("1m").build();

        assertThat(timeunit.toDateHistogramInterval(RelativeRange.create(300)))
                .isEqualTo(new DateHistogramInterval("1m"));
    }

    @Test
    public void adjustsIfMoreThanOneWeek() throws InvalidRangeParametersException {
        final TimeUnitInterval timeunit = builder().timeunit("2w").build();

        assertThat(timeunit.toDateHistogramInterval(RelativeRange.create(30 * 24 * 60 * 60)))
                .isEqualTo(new DateHistogramInterval("14d"));
    }

    @Test
    public void adjustsIfMoreThanOneMonth() throws InvalidRangeParametersException {
        final TimeUnitInterval timeunit = builder().timeunit("2M").build();

        assertThat(timeunit.toDateHistogramInterval(RelativeRange.create(365 * 24 * 60 * 60)))
                .isEqualTo(new DateHistogramInterval("60d"));
    }
}
