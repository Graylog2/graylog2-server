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

import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@RunWith(Enclosed.class)
public class TimeUnitIntervalTest {
    private static TimeUnitInterval.Builder builder() {
        return TimeUnitInterval.Builder.builder();
    }

    public static class InvalidTimeUnits {
        public static final String INVALID_TIME_UNIT_MESSAGE = "Time unit must be {quantity}{unit}, where quantity is a positive number and unit [smhdwM].";

        @Test
        public void doesNotAllowInvalidTimeUnit() {
            assertThatExceptionOfType(RuntimeException.class)
                    .isThrownBy(() -> builder().timeunit("foobar").build())
                    .withMessage(INVALID_TIME_UNIT_MESSAGE);
        }

        @Test
        public void doesNotAllowNegativeQuantity() {
            assertThatExceptionOfType(RuntimeException.class)
                    .isThrownBy(() -> builder().timeunit("-1s").build())
                    .withMessage(INVALID_TIME_UNIT_MESSAGE);
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
                    .withMessage(INVALID_TIME_UNIT_MESSAGE);
        }
    }

    @RunWith(Parameterized.class)
    public static class ValidTimeUnitIntervalsTest {
        @Parameterized.Parameters
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"1s", "1s"},
                    {"2s", "2s"},
                    {"1m", "1m"},
                    {"4m", "4m"},
                    {"1h", "1h"},
                    {"2h", "2h"},
                    {"1d", "1d"},
                    {"4d", "4d"},
                    {"1w", "1w"},
                    {"2w", "14d"},
                    {"4w", "28d"},
                    {"1M", "1M"},
                    {"2M", "60d"},
                    {"4M", "120d"}
            });
        }

        private final String timeunit;
        private final String expectedTimeunit;

        public ValidTimeUnitIntervalsTest(String timeunit, String expectedTimeunit) {
            this.timeunit = timeunit;
            this.expectedTimeunit = expectedTimeunit;
        }

        @Test
        public void allowsPositiveQuantityAndKnownUnit() throws InvalidRangeParametersException {
            final TimeUnitInterval interval = builder().timeunit(timeunit).build();

            assertThat(interval.toDateInterval(RelativeRange.create(300)).toString())
                    .isEqualTo(expectedTimeunit);
        }
    }
}
