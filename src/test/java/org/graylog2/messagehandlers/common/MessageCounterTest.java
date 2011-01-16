/**
 * Copyright 2010 Lennart Koopmann <lennart@socketfeed.com>
 *
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
 *
 */

package org.graylog2.messagehandlers.common;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * MessageCounterTest.java: Sep 17, 2010 9:01:45 PM
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class MessageCounterTest {

    @Before
    public void setUp() {
        MessageCounter.getInstance().reset(MessageCounter.ALL_HOSTS);
        MessageCounter.getInstance().resetTotalSecondCount();
    }

    @Test
    public void testGetInstance() {
        assertNotNull(MessageCounter.getInstance());
    }

    @Test
    public void testReset() {
        MessageCounter.getInstance().countUp(MessageCounter.ALL_HOSTS);
        MessageCounter.getInstance().reset(MessageCounter.ALL_HOSTS);
        assertEquals(0, MessageCounter.getInstance().getCount(MessageCounter.ALL_HOSTS));
    }

    @Test
    public void testCountUp() {
        int setCount = 4;
        for (int i = 0; i < setCount; i++) {
            MessageCounter.getInstance().countUp(MessageCounter.ALL_HOSTS);
        }
        assertEquals(setCount, MessageCounter.getInstance().getCount(MessageCounter.ALL_HOSTS));
    }

    @Test
    public void testGetCount() {
        int count_to = 15;
        for (int i = 0; i < count_to; i++) {
            MessageCounter.getInstance().countUp(MessageCounter.ALL_HOSTS);
        }

        assertEquals(count_to,MessageCounter.getInstance().getCount(MessageCounter.ALL_HOSTS));
    }

    @Test
    public void testResetTotalSecondCount() {
        MessageCounter.getInstance().countUp(MessageCounter.ALL_HOSTS);
        MessageCounter.getInstance().resetTotalSecondCount();
        assertEquals(0, MessageCounter.getInstance().getTotalSecondCount());
        assertEquals(1, MessageCounter.getInstance().getCount(MessageCounter.ALL_HOSTS));
    }

    @Test
    public void testGetTotalSecondCount() {
        int count_to = 21;
        for (int i = 0; i < count_to; i++) {
            MessageCounter.getInstance().countUp(MessageCounter.ALL_HOSTS);
        }

        assertEquals(count_to,MessageCounter.getInstance().getTotalSecondCount());
    }

    @Test
    public void testGetHighestSecondCount() {
        int count_to = 126;
        for (int i = 0; i < count_to; i++) {
            MessageCounter.getInstance().countUp(MessageCounter.ALL_HOSTS);
        }

        MessageCounter.getInstance().reset(MessageCounter.ALL_HOSTS);
        MessageCounter.getInstance().resetTotalSecondCount();

        assertEquals(count_to,MessageCounter.getInstance().getHighestSecondCount());
    }

}