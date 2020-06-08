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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import org.junit.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class AutoIntervalBoundariesTest {
    @Test
    public void rangeBoundariesMustBeConnectedAndSpanEverything() {
        final ImmutableMap<Range<Duration>, DateInterval> ranges = AutoInterval.boundaries.asMapOfRanges();
        final int rangeCount = ranges.size();
        final Range<Duration> firstRange = ranges.keySet().asList().get(0);
        assertThat(firstRange.hasLowerBound()).as("first range's lower bound should be unbounded").isFalse();

        final Range<Duration> lastRange = ranges.keySet().asList().get(rangeCount - 1);
        assertThat(lastRange.hasUpperBound()).as("last range's upper bound should be unbounded").isFalse();

        final Optional<Range<Duration>> ignored = ranges
                .keySet()
                .stream()
                .reduce((r1, r2) -> {
                    assertThat(r1.isConnected(r2))
                            .as("ranges %s and %s must be connected", r1, r2)
                            .isTrue();
                    return r2;
                });
    }
}
