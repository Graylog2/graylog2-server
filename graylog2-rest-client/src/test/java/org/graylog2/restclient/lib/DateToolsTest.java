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
package org.graylog2.restclient.lib;

import org.graylog2.restclient.models.User;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DateToolsTest {
    @Mock
    private User user;

    @Before
    public void beforeTest() {
        when(user.getTimeZone()).thenReturn(DateTimeZone.forID("Europe/Berlin"));
    }

    @After
    public void afterTest() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void testGetUserTimeZoneOffsetStandardTime() throws Exception {
        InstantMillisProvider instantMillisProvider = new InstantMillisProvider(new DateTime(2014, 10, 27, 0, 0, DateTimeZone.UTC));
        DateTimeUtils.setCurrentMillisProvider(instantMillisProvider);

        int userTimeZoneOffset = DateTools.getUserTimeZoneOffset(user);
        assertEquals(-60, userTimeZoneOffset);
    }

    @Test
    public void testGetUserTimeZoneOffsetDSTTime() throws Exception {
        InstantMillisProvider instantMillisProvider = new InstantMillisProvider(new DateTime(2014, 10, 25, 0, 0, DateTimeZone.UTC));
        DateTimeUtils.setCurrentMillisProvider(instantMillisProvider);

        int userTimeZoneOffset = DateTools.getUserTimeZoneOffset(user);
        assertEquals(-120, userTimeZoneOffset);
    }
}
