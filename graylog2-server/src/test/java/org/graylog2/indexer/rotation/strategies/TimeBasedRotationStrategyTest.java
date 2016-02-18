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

package org.graylog2.indexer.rotation.strategies;

import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.plugin.InstantMillisProvider;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static org.joda.time.Period.minutes;
import static org.joda.time.Period.seconds;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(MockitoJUnitRunner.class)
public class TimeBasedRotationStrategyTest {
    @Mock
    private ClusterConfigService clusterConfigService;

    @Mock
    private Deflector deflector;

    @Mock
    private Indices indices;

    @After
    public void resetTimeProvider() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void determineAnchor() {
        final DateTime initialTime = new DateTime(2014, 3, 15, 14, 48, 35, 0, DateTimeZone.UTC);

        final InstantMillisProvider clock = new InstantMillisProvider(initialTime);
        DateTimeUtils.setCurrentMillisProvider(clock);
        Period period;

        // should snap to 14:00:00
        period = Period.hours(1);
        final DateTime hourAnchor = TimeBasedRotationStrategy.determineRotationPeriodAnchor(null, period);
        assertEquals(hourAnchor.getHourOfDay(), 14);
        assertEquals(hourAnchor.getMinuteOfHour(), 0);
        assertEquals(hourAnchor.getSecondOfMinute(), 0);

        // should snap to 14:45:00
        period = Period.minutes(5);
        final DateTime fiveMins = TimeBasedRotationStrategy.determineRotationPeriodAnchor(null, period);
        assertEquals(fiveMins.getHourOfDay(), 14);
        assertEquals(fiveMins.getMinuteOfHour(), 45);
        assertEquals(fiveMins.getSecondOfMinute(), 0);

        // should snap to 2014-3-15 00:00:00
        period = Period.days(1).withHours(6);
        final DateTime dayAnd6Hours = TimeBasedRotationStrategy.determineRotationPeriodAnchor(null, period);
        assertEquals(dayAnd6Hours.getYear(), 2014);
        assertEquals(dayAnd6Hours.getMonthOfYear(), 3);
        assertEquals(dayAnd6Hours.getDayOfMonth(), 15);
        assertEquals(dayAnd6Hours.getHourOfDay(), 0);
        assertEquals(dayAnd6Hours.getMinuteOfHour(), 0);
        assertEquals(dayAnd6Hours.getSecondOfMinute(), 0);

        period = Period.days(30);
        final DateTime thirtyDays = TimeBasedRotationStrategy.determineRotationPeriodAnchor(null, period);
        assertEquals(thirtyDays.getYear(), 2014);
        assertEquals(thirtyDays.getMonthOfYear(), 2);
        assertEquals(thirtyDays.getDayOfMonth(), 17);
        assertEquals(thirtyDays.getHourOfDay(), 0);
        assertEquals(thirtyDays.getMinuteOfHour(), 0);
        assertEquals(thirtyDays.getSecondOfMinute(), 0);

        period = Period.hours(1);
        final DateTime diffAnchor = TimeBasedRotationStrategy.determineRotationPeriodAnchor(initialTime.minusMinutes(61), period);
        assertEquals(diffAnchor.getYear(), 2014);
        assertEquals(diffAnchor.getMonthOfYear(), 3);
        assertEquals(diffAnchor.getDayOfMonth(), 15);
        assertEquals(diffAnchor.getHourOfDay(), 13);
        assertEquals(diffAnchor.getMinuteOfHour(), 0);
        assertEquals(diffAnchor.getSecondOfMinute(), 0);

    }

    @Test
    public void shouldRotateHourly() throws Exception {
        final DateTime initialTime = new DateTime(2014, 1, 1, 1, 59, 59, 0, DateTimeZone.UTC);
        final Period period = Period.hours(1);

        final InstantMillisProvider clock = new InstantMillisProvider(initialTime);
        DateTimeUtils.setCurrentMillisProvider(clock);

        when(indices.indexCreationDate(anyString())).thenReturn(initialTime.minus(Period.minutes(5)));
        when(deflector.getNewestTargetName()).thenReturn("ignored");
        when(clusterConfigService.get(TimeBasedRotationStrategyConfig.class)).thenReturn(Optional.of(TimeBasedRotationStrategyConfig.create(period)));

        final TimeBasedRotationStrategy hourlyRotation = new TimeBasedRotationStrategy(indices, deflector, clusterConfigService);

        // Should not rotate the first index.
        hourlyRotation.rotate();
        verify(deflector, never()).cycle();
        reset(deflector);

        clock.tick(seconds(2));

        // Crossed rotation period.
        hourlyRotation.rotate();
        verify(deflector, times(1)).cycle();
        reset(deflector);

        clock.tick(seconds(2));

        // Did not cross rotation period.
        hourlyRotation.rotate();
        verify(deflector, never()).cycle();
        reset(deflector);
    }

    @Test
    public void shouldRotateNonIntegralPeriod() throws Exception {
        // start 5 minutes before full hour
        final DateTime initialTime = new DateTime(2014, 1, 1, 1, 55, 0, 0, DateTimeZone.UTC);
        final Period period = Period.minutes(10);

        final InstantMillisProvider clock = new InstantMillisProvider(initialTime);
        DateTimeUtils.setCurrentMillisProvider(clock);
        when(indices.indexCreationDate(anyString())).thenReturn(initialTime.minus(Period.minutes(11)));
        when(deflector.getNewestTargetName()).thenReturn("ignored");
        when(clusterConfigService.get(TimeBasedRotationStrategyConfig.class)).thenReturn(Optional.of(TimeBasedRotationStrategyConfig.create(period)));

        final TimeBasedRotationStrategy tenMinRotation = new TimeBasedRotationStrategy(indices, deflector, clusterConfigService);

        // Should rotate the first index.
        // time is 01:55:00, index was created at 01:44:00, so we missed one period, and should rotate
        tenMinRotation.rotate();
        verify(deflector, times(1)).cycle();
        reset(deflector);

        // advance time to 01:55:01
        clock.tick(seconds(1));

        // Did not cross rotation period.
        tenMinRotation.rotate();
        verify(deflector, never()).cycle();
        reset(deflector);

        // advance time to 02:00:00
        clock.tick(minutes(4).withSeconds(59));

        // Crossed rotation period.
        tenMinRotation.rotate();
        verify(deflector, times(1)).cycle();
        reset(deflector);

        // advance time multiple rotation periods into the future
        // to time 02:51:00
        clock.tick(minutes(51));

        // Crossed multiple rotation periods.
        tenMinRotation.rotate();
        verify(deflector, times(1)).cycle();
        reset(deflector);

        // move time to 2:52:00
        // this should not cycle again, because next valid rotation time is 3:00:00
        clock.tick(minutes(1));
        tenMinRotation.rotate();
        verify(deflector, never()).cycle();
        reset(deflector);
    }

}
