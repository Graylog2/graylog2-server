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

import org.graylog2.audit.AuditEventSender;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.plugin.InstantMillisProvider;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.joda.time.Period.minutes;
import static org.joda.time.Period.seconds;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class TimeBasedRotationStrategyTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Mock
    private IndexSet indexSet;

    @Mock
    private IndexSetConfig indexSetConfig;

    @Mock
    private Indices indices;

    @Mock
    private NodeId nodeId;

    @Mock
    private AuditEventSender auditEventSender;

    private TimeBasedRotationStrategy rotationStrategy;

    @Before
    public void setUp() {
        when(indexSetConfig.id()).thenReturn("index-set-id");
        rotationStrategy = new TimeBasedRotationStrategy(indices, nodeId, auditEventSender);
    }

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
        period = minutes(5);
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

        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSetConfig.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.create(period));
        when(indices.indexCreationDate(anyString())).thenReturn(initialTime.minus(minutes(5)));

        // Should not rotate the first index.
        when(indexSet.getNewestIndex()).thenReturn("ignored");
        rotationStrategy.rotate(indexSet);
        verify(indexSet, never()).cycle();
        reset(indexSet);

        clock.tick(seconds(2));

        // Crossed rotation period.
        when(indexSet.getNewestIndex()).thenReturn("ignored");
        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSetConfig.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.create(period));
        rotationStrategy.rotate(indexSet);
        verify(indexSet, times(1)).cycle();
        reset(indexSet);

        clock.tick(seconds(2));

        // Did not cross rotation period.
        when(indexSet.getNewestIndex()).thenReturn("ignored");
        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSetConfig.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.create(period));
        rotationStrategy.rotate(indexSet);
        verify(indexSet, never()).cycle();
        reset(indexSet);
    }

    @Test
    public void shouldRotateNonIntegralPeriod() throws Exception {
        // start 5 minutes before full hour
        final DateTime initialTime = new DateTime(2014, 1, 1, 1, 55, 0, 0, DateTimeZone.UTC);
        final Period period = minutes(10);

        final InstantMillisProvider clock = new InstantMillisProvider(initialTime);
        DateTimeUtils.setCurrentMillisProvider(clock);
        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSetConfig.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.create(period));
        when(indices.indexCreationDate(anyString())).thenReturn(initialTime.minus(minutes(11)));

        // Should rotate the first index.
        // time is 01:55:00, index was created at 01:44:00, so we missed one period, and should rotate
        when(indexSet.getNewestIndex()).thenReturn("ignored");
        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSetConfig.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.create(period));
        rotationStrategy.rotate(indexSet);
        verify(indexSet, times(1)).cycle();
        reset(indexSet);

        // advance time to 01:55:01
        clock.tick(seconds(1));

        // Did not cross rotation period.
        when(indexSet.getNewestIndex()).thenReturn("ignored");
        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSetConfig.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.create(period));
        rotationStrategy.rotate(indexSet);
        verify(indexSet, never()).cycle();
        reset(indexSet);

        // advance time to 02:00:00
        clock.tick(minutes(4).withSeconds(59));

        // Crossed rotation period.
        when(indexSet.getNewestIndex()).thenReturn("ignored");
        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSetConfig.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.create(period));
        rotationStrategy.rotate(indexSet);
        verify(indexSet, times(1)).cycle();
        reset(indexSet);

        // advance time multiple rotation periods into the future
        // to time 02:51:00
        clock.tick(minutes(51));

        // Crossed multiple rotation periods.
        when(indexSet.getNewestIndex()).thenReturn("ignored");
        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSetConfig.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.create(period));
        rotationStrategy.rotate(indexSet);
        verify(indexSet, times(1)).cycle();
        reset(indexSet);

        // move time to 2:52:00
        // this should not cycle again, because next valid rotation time is 3:00:00
        clock.tick(minutes(1));
        when(indexSet.getNewestIndex()).thenReturn("ignored");
        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSetConfig.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.create(period));
        rotationStrategy.rotate(indexSet);
        verify(indexSet, never()).cycle();
        reset(indexSet);
    }

    @Test
    public void shouldRotateThrowsNPEIfIndexSetConfigIsNull() throws Exception {
        when(indexSet.getConfig()).thenReturn(null);
        when(indexSet.getNewestIndex()).thenReturn("ignored");

        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("Index set configuration must not be null");

        rotationStrategy.rotate(indexSet);
    }

    @Test
    public void shouldRotateThrowsISEIfIndexIsNull() throws Exception {
        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSet.getNewestIndex()).thenReturn(null);

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Index name must not be null or empty");

        rotationStrategy.rotate(indexSet);
    }

    @Test
    public void shouldRotateThrowsISEIfIndexIsEmpty() throws Exception {
        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSet.getNewestIndex()).thenReturn("");

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Index name must not be null or empty");

        rotationStrategy.rotate(indexSet);
    }

    @Test
    public void shouldRotateThrowsISEIfIndexSetIdIsNull() throws Exception {
        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSetConfig.id()).thenReturn(null);
        when(indexSet.getNewestIndex()).thenReturn("ignored");

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Index set ID must not be null or empty");

        rotationStrategy.rotate(indexSet);
    }

    @Test
    public void shouldRotateThrowsISEIfIndexSetIdIsEmpty() throws Exception {
        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSetConfig.id()).thenReturn("");
        when(indexSet.getNewestIndex()).thenReturn("ignored");

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Index set ID must not be null or empty");

        rotationStrategy.rotate(indexSet);
    }

    @Test
    public void shouldRotateThrowsISEIfRotationStrategyHasIncorrectType() throws Exception {
        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSet.getNewestIndex()).thenReturn("ignored");
        when(indexSetConfig.rotationStrategy()).thenReturn(MessageCountRotationStrategyConfig.createDefault());

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Invalid rotation strategy config");

        rotationStrategy.rotate(indexSet);
    }

    @Test
    public void shouldRotateConcurrently() throws Exception {
        final DateTime initialTime = new DateTime(2014, 1, 1, 1, 59, 59, 0, DateTimeZone.UTC);
        final Period period = Period.hours(1);

        final InstantMillisProvider clock = new InstantMillisProvider(initialTime);
        DateTimeUtils.setCurrentMillisProvider(clock);

        final IndexSet indexSet1 = mock(IndexSet.class);
        final IndexSet indexSet2 = mock(IndexSet.class);
        final IndexSetConfig indexSetConfig1 = mock(IndexSetConfig.class);
        final IndexSetConfig indexSetConfig2 = mock(IndexSetConfig.class);

        when(indexSetConfig1.id()).thenReturn("id1");
        when(indexSetConfig2.id()).thenReturn("id2");
        
        when(indexSet1.getConfig()).thenReturn(indexSetConfig1);
        when(indexSet2.getConfig()).thenReturn(indexSetConfig2);

        when(indexSetConfig1.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.create(period));
        when(indexSetConfig2.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.create(period));

        when(indices.indexCreationDate(anyString())).thenReturn(initialTime.minus(minutes(5)));

        // Should not rotate the initial index.
        when(indexSet1.getNewestIndex()).thenReturn("index1");
        rotationStrategy.rotate(indexSet1);
        verify(indexSet1, never()).cycle();
        reset(indexSet1);

        when(indexSet2.getNewestIndex()).thenReturn("index2");
        rotationStrategy.rotate(indexSet2);
        verify(indexSet2, never()).cycle();
        reset(indexSet2);

        clock.tick(seconds(2));

        // Crossed rotation period.
        when(indexSet1.getNewestIndex()).thenReturn("index1");
        when(indexSet1.getConfig()).thenReturn(indexSetConfig1);
        when(indexSetConfig1.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.create(period));
        rotationStrategy.rotate(indexSet1);
        verify(indexSet1, times(1)).cycle();
        reset(indexSet1);

        when(indexSet2.getNewestIndex()).thenReturn("index2");
        when(indexSet2.getConfig()).thenReturn(indexSetConfig2);
        when(indexSetConfig2.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.create(period));
        rotationStrategy.rotate(indexSet2);
        verify(indexSet2, times(1)).cycle();
        reset(indexSet2);

        clock.tick(seconds(2));

        // Did not cross rotation period.
        when(indexSet1.getNewestIndex()).thenReturn("index1");
        when(indexSet1.getConfig()).thenReturn(indexSetConfig1);
        when(indexSetConfig1.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.create(period));
        rotationStrategy.rotate(indexSet1);
        verify(indexSet1, never()).cycle();
        reset(indexSet1);

        when(indexSet2.getNewestIndex()).thenReturn("index2");
        when(indexSet2.getConfig()).thenReturn(indexSetConfig2);
        when(indexSetConfig2.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.create(period));
        rotationStrategy.rotate(indexSet2);
        verify(indexSet2, never()).cycle();
        reset(indexSet2);
    }
}
