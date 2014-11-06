/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.restclient.lib;

import org.graylog2.restclient.models.User;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;

public class DateToolsTest {
    private User user;
    @BeforeTest
    public void beforeTest() {
        this.user = mock(User.class);
        when(user.getTimeZone()).thenReturn(DateTimeZone.forID("Europe/Berlin"));
    }

    @AfterTest
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
