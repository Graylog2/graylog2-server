/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.indexer.rotation.strategies;

import com.google.common.collect.ImmutableSet;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.plugin.InstantMillisProvider;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.system.SimpleNodeId;
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

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.time.Period.hours;
import static org.joda.time.Period.minutes;
import static org.joda.time.Period.seconds;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class TimeBasedRotationStrategyTest {
    public static final String IGNORED = "ignored";
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

    private final NodeId nodeId = new SimpleNodeId("5ca1ab1e-0000-4000-a000-000000000000");

    @Mock
    private AuditEventSender auditEventSender;

    private ElasticsearchConfiguration configuration = new ElasticsearchConfiguration();
    private TimeBasedRotationStrategy rotationStrategy;

    @Before
    public void setUp() {
        when(indexSetConfig.id()).thenReturn("index-set-id");
        when(indexSetConfig.title()).thenReturn("index-set-title");
        when(indices.getIndices(eq(indexSet))).thenReturn(Collections.singleton(IGNORED));
        when(indices.numberOfMessages(eq(IGNORED))).thenReturn(20L);
        when(indices.isOpen(anyString())).thenReturn(true);
        rotationStrategy = new TimeBasedRotationStrategy(indices, nodeId, auditEventSender, configuration);
    }

    @After
    public void resetTimeProvider() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void anchorCalculationShouldWorkWhenLastAnchorIsNotUTC() {
        final DateTime initialTime = new DateTime(2020, 7, 31, 14, 48, 35, 0, DateTimeZone.UTC);

        final InstantMillisProvider clock = new InstantMillisProvider(initialTime);
        DateTimeUtils.setCurrentMillisProvider(clock);

        Period period = Period.months(1);
        DateTime lastAnchor = initialTime.minusHours(1).withZone(DateTimeZone.forOffsetHours(2));
        final DateTime monthAnchor = TimeBasedRotationStrategy.determineRotationPeriodAnchor(lastAnchor, period);
        assertThat(monthAnchor).isEqualTo(DateTime.parse("2020-07-01T00:00:00.000Z"));
    }

    @Test
    public void determineAnchor() {
        final DateTime initialTime = new DateTime(2014, 3, 15, 14, 48, 35, 0, DateTimeZone.UTC);

        final InstantMillisProvider clock = new InstantMillisProvider(initialTime);
        DateTimeUtils.setCurrentMillisProvider(clock);
        Period period;

        // should snap to 14:00:00
        period = hours(1);
        final DateTime hourAnchor = TimeBasedRotationStrategy.determineRotationPeriodAnchor(null, period);
        assertEquals(14, hourAnchor.getHourOfDay());
        assertEquals(0, hourAnchor.getMinuteOfHour());
        assertEquals(0, hourAnchor.getSecondOfMinute());

        // should snap to 14:45:00
        period = minutes(5);
        final DateTime fiveMins = TimeBasedRotationStrategy.determineRotationPeriodAnchor(null, period);
        assertEquals(14, fiveMins.getHourOfDay());
        assertEquals(45, fiveMins.getMinuteOfHour());
        assertEquals(0, fiveMins.getSecondOfMinute());

        // should snap to 2014-3-15 00:00:00
        period = Period.days(1).withHours(6);
        final DateTime dayAnd6Hours = TimeBasedRotationStrategy.determineRotationPeriodAnchor(null, period);
        assertEquals(2014, dayAnd6Hours.getYear());
        assertEquals(3, dayAnd6Hours.getMonthOfYear());
        assertEquals(15, dayAnd6Hours.getDayOfMonth());
        assertEquals(0, dayAnd6Hours.getHourOfDay());
        assertEquals(0, dayAnd6Hours.getMinuteOfHour());
        assertEquals(0, dayAnd6Hours.getSecondOfMinute());

        period = Period.days(30);
        final DateTime thirtyDays = TimeBasedRotationStrategy.determineRotationPeriodAnchor(null, period);
        assertEquals(2014, thirtyDays.getYear());
        assertEquals(2, thirtyDays.getMonthOfYear());
        assertEquals(17, thirtyDays.getDayOfMonth());
        assertEquals(0, thirtyDays.getHourOfDay());
        assertEquals(0, thirtyDays.getMinuteOfHour());
        assertEquals(0, thirtyDays.getSecondOfMinute());

        period = hours(1);
        final DateTime diffAnchor = TimeBasedRotationStrategy.determineRotationPeriodAnchor(initialTime.minusMinutes(61), period);
        assertEquals(2014, diffAnchor.getYear());
        assertEquals(3, diffAnchor.getMonthOfYear());
        assertEquals(15, diffAnchor.getDayOfMonth());
        assertEquals(13, diffAnchor.getHourOfDay());
        assertEquals(0, diffAnchor.getMinuteOfHour());
        assertEquals(0, diffAnchor.getSecondOfMinute());

    }

    @Test
    public void shouldRotateHourly() throws Exception {
        final DateTime initialTime = new DateTime(2014, 1, 1, 1, 59, 59, 0, DateTimeZone.UTC);
        final Period period = hours(1);

        final InstantMillisProvider clock = new InstantMillisProvider(initialTime);
        DateTimeUtils.setCurrentMillisProvider(clock);

        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSetConfig.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.builder().rotationPeriod(period).build());
        when(indices.indexCreationDate(anyString())).thenReturn(Optional.of(initialTime.minus(minutes(5))));

        // Should not rotate the first index.
        when(indexSet.getNewestIndex()).thenReturn(IGNORED);
        rotationStrategy.rotate(indexSet);
        verify(indexSet, never()).cycle();
        reset(indexSet);

        clock.tick(seconds(2));

        // Crossed rotation period.
        when(indexSet.getNewestIndex()).thenReturn(IGNORED);
        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSetConfig.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.builder().rotationPeriod(period).build());
        rotationStrategy.rotate(indexSet);
        verify(indexSet, times(1)).cycle();
        reset(indexSet);

        clock.tick(seconds(2));

        // Did not cross rotation period.
        when(indexSet.getNewestIndex()).thenReturn(IGNORED);
        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSetConfig.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.builder().rotationPeriod(period).build());
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
        when(indexSetConfig.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.builder().rotationPeriod(period).build());
        when(indices.indexCreationDate(anyString())).thenReturn(Optional.of(initialTime.minus(minutes(11))));

        // Should rotate the first index.
        // time is 01:55:00, index was created at 01:44:00, so we missed one period, and should rotate
        when(indexSet.getNewestIndex()).thenReturn(IGNORED);
        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSetConfig.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.builder().rotationPeriod(period).build());
        rotationStrategy.rotate(indexSet);
        verify(indexSet, times(1)).cycle();
        reset(indexSet);

        // advance time to 01:55:01
        clock.tick(seconds(1));

        // Did not cross rotation period.
        when(indexSet.getNewestIndex()).thenReturn(IGNORED);
        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSetConfig.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.builder().rotationPeriod(period).build());
        rotationStrategy.rotate(indexSet);
        verify(indexSet, never()).cycle();
        reset(indexSet);

        // advance time to 02:00:00
        clock.tick(minutes(4).withSeconds(59));

        // Crossed rotation period.
        when(indexSet.getNewestIndex()).thenReturn(IGNORED);
        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSetConfig.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.builder().rotationPeriod(period).build());
        rotationStrategy.rotate(indexSet);
        verify(indexSet, times(1)).cycle();
        reset(indexSet);

        // advance time multiple rotation periods into the future
        // to time 02:51:00
        clock.tick(minutes(51));

        // Crossed multiple rotation periods.
        when(indexSet.getNewestIndex()).thenReturn(IGNORED);
        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSetConfig.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.builder().rotationPeriod(period).build());
        rotationStrategy.rotate(indexSet);
        verify(indexSet, times(1)).cycle();
        reset(indexSet);

        // move time to 2:52:00
        // this should not cycle again, because next valid rotation time is 3:00:00
        clock.tick(minutes(1));
        when(indexSet.getNewestIndex()).thenReturn(IGNORED);
        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSetConfig.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.builder().rotationPeriod(period).build());
        rotationStrategy.rotate(indexSet);
        verify(indexSet, never()).cycle();
        reset(indexSet);
    }

    @Test
    public void shouldRotateThrowsNPEIfIndexSetConfigIsNull() throws Exception {
        when(indexSet.getConfig()).thenReturn(null);
        when(indexSet.getNewestIndex()).thenReturn(IGNORED);

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
        when(indexSet.getNewestIndex()).thenReturn(IGNORED);

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Index set ID must not be null or empty");

        rotationStrategy.rotate(indexSet);
    }

    @Test
    public void shouldRotateThrowsISEIfIndexSetIdIsEmpty() throws Exception {
        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSetConfig.id()).thenReturn("");
        when(indexSet.getNewestIndex()).thenReturn(IGNORED);

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Index set ID must not be null or empty");

        rotationStrategy.rotate(indexSet);
    }

    @Test
    public void shouldRotateThrowsISEIfRotationStrategyHasIncorrectType() throws Exception {
        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSet.getNewestIndex()).thenReturn(IGNORED);
        when(indexSetConfig.rotationStrategy()).thenReturn(MessageCountRotationStrategyConfig.createDefault());

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Invalid rotation strategy config");

        rotationStrategy.rotate(indexSet);
    }

    @Test
    public void shouldRotateConcurrently() throws Exception {
        final DateTime initialTime = new DateTime(2014, 1, 1, 1, 59, 59, 0, DateTimeZone.UTC);
        final Period period = hours(1);

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

        when(indexSetConfig1.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.builder().rotationPeriod(period).build());
        when(indexSetConfig2.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.builder().rotationPeriod(period).build());

        when(indices.indexCreationDate(anyString())).thenReturn(Optional.of(initialTime.minus(minutes(5))));

        when(indices.getIndices(eq(indexSet1))).thenReturn(Collections.singleton("index1"));
        when(indices.numberOfMessages(eq("index1"))).thenReturn(20L);

        // Should not rotate the initial index.
        when(indexSet1.getNewestIndex()).thenReturn("index1");
        rotationStrategy.rotate(indexSet1);
        verify(indexSet1, never()).cycle();
        reset(indexSet1);

        when(indices.getIndices(eq(indexSet2))).thenReturn(Collections.singleton("index2"));
        when(indices.numberOfMessages(eq("index2"))).thenReturn(20L);

        when(indexSet2.getNewestIndex()).thenReturn("index2");
        rotationStrategy.rotate(indexSet2);
        verify(indexSet2, never()).cycle();
        reset(indexSet2);

        clock.tick(seconds(2));

        // Crossed rotation period.
        when(indexSet1.getNewestIndex()).thenReturn("index1");
        when(indexSet1.getConfig()).thenReturn(indexSetConfig1);
        when(indexSetConfig1.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.builder().rotationPeriod(period).build());
        rotationStrategy.rotate(indexSet1);
        verify(indexSet1, times(1)).cycle();
        reset(indexSet1);

        when(indexSet2.getNewestIndex()).thenReturn("index2");
        when(indexSet2.getConfig()).thenReturn(indexSetConfig2);
        when(indexSetConfig2.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.builder().rotationPeriod(period).build());
        rotationStrategy.rotate(indexSet2);
        verify(indexSet2, times(1)).cycle();
        reset(indexSet2);

        clock.tick(seconds(2));

        // Did not cross rotation period.
        when(indexSet1.getNewestIndex()).thenReturn("index1");
        when(indexSet1.getConfig()).thenReturn(indexSetConfig1);
        when(indexSetConfig1.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.builder().rotationPeriod(period).build());
        rotationStrategy.rotate(indexSet1);
        verify(indexSet1, never()).cycle();
        reset(indexSet1);

        when(indexSet2.getNewestIndex()).thenReturn("index2");
        when(indexSet2.getConfig()).thenReturn(indexSetConfig2);
        when(indexSetConfig2.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.builder().rotationPeriod(period).build());
        rotationStrategy.rotate(indexSet2);
        verify(indexSet2, never()).cycle();
        reset(indexSet2);
    }

    // Without any previous state, rotation for a mixed period (e.g. 1 hour + 30 minutes) behaves differently than it
    // does with previous state. This is not ideal and this test demonstrates the behaviour by also acting as a test for
    // the #reset method. The ultimate goal would be to remove state, the #reset method and this test altogether.
    @Test
    public void testResetWithMixedPeriod() throws Exception {
        final DateTime initialTime = new DateTime(2014, 1, 1, 1, 0, 0, 0, DateTimeZone.UTC);
        final Period period = hours(1).plusMinutes(30);

        final InstantMillisProvider clock = new InstantMillisProvider(initialTime);
        DateTimeUtils.setCurrentMillisProvider(clock);

        when(indexSet.getNewestIndex()).thenReturn(IGNORED);
        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSetConfig.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.builder().rotationPeriod(period).build());
        when(indices.indexCreationDate(anyString())).thenReturn(Optional.of(initialTime.minus(minutes(1))));

        clock.tick(minutes(31));

        // current time: 01:31
        // index creation: 00:59 -> anchor: 00:00

        // ideally we wouldn't rotate, because the index is only 32 minutes old
        rotationStrategy.rotate(indexSet);
        verify(indexSet, times(1)).cycle();

        reset(indexSet);
        reset(indices);

        when(indexSet.getNewestIndex()).thenReturn(IGNORED);
        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSetConfig.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.builder().rotationPeriod(period).build());
        when(indices.indexCreationDate(anyString())).thenReturn(Optional.of(new DateTime(clock.getMillis(), DateTimeZone.UTC)));

        clock.tick(hours(1));

        rotationStrategy.reset();

        // current time: 02:31
        // index creation: 01:31 -> anchor 01:00 (without resetting it would be 01:30)

        // ideally we wouldn't rotate here, because the index is only 1 hour old
        when(indices.getIndices(eq(indexSet))).thenReturn(Collections.singleton(IGNORED));
        when(indices.numberOfMessages(eq(IGNORED))).thenReturn(20L);
        when(indices.isOpen(anyString())).thenReturn(true);
        rotationStrategy.rotate(indexSet);
        verify(indexSet, times(1)).cycle();
    }

    @Test
    public void shouldSkipRotationOnEmptyIndexSet() throws Exception {
        final DateTime initialTime = new DateTime(2022, 7, 21, 13, 00, 00, 0, DateTimeZone.UTC);
        final Period period = hours(1);
        final InstantMillisProvider clock = new InstantMillisProvider(initialTime);
        DateTimeUtils.setCurrentMillisProvider(clock);

        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indices.indexCreationDate(anyString())).thenReturn(Optional.of(initialTime.minus(minutes(11))));

        when(indexSet.getNewestIndex()).thenReturn(IGNORED);
        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSetConfig.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.builder().rotationPeriod(period).build());

        when(indices.numberOfMessages(anyString())).thenReturn(0L);
        when(indices.getIndices(eq(indexSet))).thenReturn(ImmutableSet.of("emptyIndex", IGNORED));

        rotationStrategy.rotate(indexSet);
        clock.tick(minutes(60));
        rotationStrategy.rotate(indexSet);

        verify(indexSet, never()).cycle();

        //rotates after message detected in any index
        when(indices.numberOfMessages(eq(IGNORED))).thenReturn(20L);

        rotationStrategy.rotate(indexSet);
        verify(indexSet, times(1)).cycle();
    }

    @Test
    public void testRotationOnEmptyIndexSetWhenEnabled() throws Exception {
        final DateTime initialTime = new DateTime(2022, 7, 21, 13, 00, 00, 0, DateTimeZone.UTC);
        final Period period = hours(1);
        final InstantMillisProvider clock = new InstantMillisProvider(initialTime);
        DateTimeUtils.setCurrentMillisProvider(clock);

        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSetConfig.rotationStrategy()).thenReturn(TimeBasedRotationStrategyConfig.builder().rotateEmptyIndexSet(true).rotationPeriod(period).build());
        when(indices.indexCreationDate(anyString())).thenReturn(Optional.of(initialTime.minus(minutes(11))));

        when(indexSet.getNewestIndex()).thenReturn(IGNORED);
        when(indexSet.getConfig()).thenReturn(indexSetConfig);

        when(indices.numberOfMessages(anyString())).thenReturn(0L);
        when(indices.getIndices(eq(indexSet))).thenReturn(ImmutableSet.of("emptyIndex", IGNORED));
        clock.tick(minutes(60));
        rotationStrategy.rotate(indexSet);
        verify(indexSet, times(1)).cycle();
    }
}
