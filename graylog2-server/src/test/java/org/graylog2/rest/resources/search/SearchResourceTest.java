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
package org.graylog2.rest.resources.search;

import com.codahale.metrics.MetricRegistry;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.indexer.searches.timeranges.TimeRange;
import org.graylog2.plugin.database.users.User;
import org.graylog2.users.RoleServiceImpl;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SearchResourceTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private Searches searches;
    @Mock
    private User user;

    private SearchResource searchResource;
    private Duration timeRangeLimit;

    @Before
    public void setUp() {
        timeRangeLimit = Duration.standardDays(1L);
        when(user.isLocalAdmin()).thenReturn(false);
        when(user.getRoleIds()).thenReturn(Collections.singleton(RoleServiceImpl.READER_ROLENAME));
        searchResource = new SearchResource(searches, new MetricRegistry(), timeRangeLimit) {
            @Override
            protected User getCurrentUser() {
                return user;
            }
        };
    }

    @Test
    public void restrictTimeRangeReturnsGivenTimeRangeWithinLimit() {
        final DateTime from = new DateTime(2015, 1, 15, 12, 0, DateTimeZone.UTC);
        final DateTime to = from.plusHours(1);
        final TimeRange timeRange = new AbsoluteRange(from, to);

        final TimeRange restrictedTimeRange = searchResource.restrictTimeRange(timeRange);
        assertThat(restrictedTimeRange).isNotNull();
        assertThat(restrictedTimeRange.getFrom()).isEqualTo(from);
        assertThat(restrictedTimeRange.getTo()).isEqualTo(to);
    }

    @Test
    public void restrictTimeRangeReturnsGivenTimeRangeIfNoLimitHasBeenSet() {
        final SearchResource resource = new SearchResource(searches, new MetricRegistry(), null) {
            @Override
            protected User getCurrentUser() {
                return user;
            }
        };

        final DateTime from = new DateTime(2015, 1, 15, 12, 0, DateTimeZone.UTC);
        final DateTime to = from.plusYears(1);
        final TimeRange timeRange = new AbsoluteRange(from, to);

        final TimeRange restrictedTimeRange = resource.restrictTimeRange(timeRange);
        assertThat(restrictedTimeRange).isNotNull();
        assertThat(restrictedTimeRange.getFrom()).isEqualTo(from);
        assertThat(restrictedTimeRange.getTo()).isEqualTo(to);
    }

    @Test
    public void restrictTimeRangeReturnsLimitedTimeRange() {
        final DateTime from = new DateTime(2015, 1, 15, 12, 0, DateTimeZone.UTC);
        final DateTime to = from.plus(timeRangeLimit.multipliedBy(2L));
        final TimeRange timeRange = new AbsoluteRange(from, to);
        final TimeRange restrictedTimeRange = searchResource.restrictTimeRange(timeRange);

        assertThat(restrictedTimeRange).isNotNull();
        assertThat(restrictedTimeRange.getFrom()).isEqualTo(to.minus(timeRangeLimit));
        assertThat(restrictedTimeRange.getTo()).isEqualTo(to);
    }

    @Test
    public void restrictTimeRangeDoesNotLimitAdminUser() {
        final User admin = mock(User.class);
        when(admin.isLocalAdmin()).thenReturn(false);
        when(admin.getRoleIds()).thenReturn(Collections.singleton(RoleServiceImpl.ADMIN_ROLENAME));

        final SearchResource resource = new SearchResource(searches, new MetricRegistry(), Duration.standardHours(1L)) {
            @Override
            protected User getCurrentUser() {
                return admin;
            }
        };

        final DateTime from = new DateTime(2015, 1, 15, 12, 0, DateTimeZone.UTC);
        final DateTime to = from.plusYears(1);
        final TimeRange timeRange = new AbsoluteRange(from, to);
        final TimeRange restrictedTimeRange = resource.restrictTimeRange(timeRange);

        assertThat(restrictedTimeRange).isNotNull();
        assertThat(restrictedTimeRange.getFrom()).isEqualTo(from);
        assertThat(restrictedTimeRange.getTo()).isEqualTo(to);
    }

    @Test
    public void restrictTimeRangeDoesNotLimitLocalAdmin() {
        final User admin = mock(User.class);
        when(admin.isLocalAdmin()).thenReturn(true);
        when(admin.getRoleIds()).thenReturn(Collections.singleton(RoleServiceImpl.READER_ROLENAME));

        final SearchResource resource = new SearchResource(searches, new MetricRegistry(), Duration.standardHours(1L)) {
            @Override
            protected User getCurrentUser() {
                return admin;
            }
        };

        final DateTime from = new DateTime(2015, 1, 15, 12, 0, DateTimeZone.UTC);
        final DateTime to = from.plusYears(1);
        final TimeRange timeRange = new AbsoluteRange(from, to);
        final TimeRange restrictedTimeRange = resource.restrictTimeRange(timeRange);

        assertThat(restrictedTimeRange).isNotNull();
        assertThat(restrictedTimeRange.getFrom()).isEqualTo(from);
        assertThat(restrictedTimeRange.getTo()).isEqualTo(to);
    }
}