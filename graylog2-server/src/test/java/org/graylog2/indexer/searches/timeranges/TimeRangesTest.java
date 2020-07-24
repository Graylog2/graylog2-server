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
package org.graylog2.indexer.searches.timeranges;

import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.KeywordRange;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class TimeRangesTest {
    @Test
    public void toSecondsHandlesIncompleteTimeRange() throws Exception {
        assertThat(TimeRanges.toSeconds(new TimeRange() {
            @Override
            public String type() {
                return AbsoluteRange.ABSOLUTE;
            }

            @Override
            public DateTime getFrom() {
                return DateTime.now(DateTimeZone.UTC);
            }

            @Override
            public DateTime getTo() {
                return null;
            }
        })).isEqualTo(0);
        assertThat(TimeRanges.toSeconds(new TimeRange() {
            @Override
            public String type() {
                return AbsoluteRange.ABSOLUTE;
            }

            @Override
            public DateTime getFrom() {
                return null;
            }

            @Override
            public DateTime getTo() {
                return DateTime.now(DateTimeZone.UTC);
            }
        })).isEqualTo(0);
    }

    @Test
    public void toSecondsReturnsCorrectNumberOfSeconds() throws Exception {
        DateTime from = DateTime.now(DateTimeZone.UTC);
        DateTime to = from.plusMinutes(5);

        assertThat(TimeRanges.toSeconds(AbsoluteRange.create(from, to))).isEqualTo(300);
        assertThat(TimeRanges.toSeconds(RelativeRange.create(300))).isEqualTo(300);
        assertThat(TimeRanges.toSeconds(KeywordRange.create("last 5 minutes"))).isEqualTo(300);
    }
}
