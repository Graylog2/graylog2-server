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

import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


public class TimeRangesTest {
    @Test
    public void toSecondsHandlesIncompleteTimeRange() throws Exception {
        assertThat(TimeRanges.toSeconds(new TimeRange() {
            @Override
            public Type getType() {
                return Type.ABSOLUTE;
            }

            @Override
            public Map<String, Object> getPersistedConfig() {
                return Collections.emptyMap();
            }

            @Override
            public DateTime getFrom() {
                return DateTime.now();
            }

            @Override
            public DateTime getTo() {
                return null;
            }
        })).isEqualTo(0);
        assertThat(TimeRanges.toSeconds(new TimeRange() {
            @Override
            public Type getType() {
                return Type.ABSOLUTE;
            }

            @Override
            public Map<String, Object> getPersistedConfig() {
                return Collections.emptyMap();
            }

            @Override
            public DateTime getFrom() {
                return null;
            }

            @Override
            public DateTime getTo() {
                return DateTime.now();
            }
        })).isEqualTo(0);
    }

    @Test
    public void toSecondsReturnsCorrectNumberOfSeconds() throws Exception {
        DateTime from = DateTime.now();
        DateTime to = from.plusMinutes(5);

        assertThat(TimeRanges.toSeconds(new AbsoluteRange(from, to))).isEqualTo(300);
        assertThat(TimeRanges.toSeconds(new RelativeRange(300))).isEqualTo(300);
        assertThat(TimeRanges.toSeconds(new KeywordRange("last 5 minutes"))).isEqualTo(300);
    }
}