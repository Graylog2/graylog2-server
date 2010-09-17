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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * MessageCounterTest.java: Sep 17, 2010 9:01:45 PM
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class MessageCounterTest {

    public MessageCounterTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getInstance method, of class MessageCounter.
     */
    @Test
    public void testGetInstance() {
        assertNotNull(MessageCounter.getInstance());
    }

    /**
     * Test of reset method, of class MessageCounter.
     */
    @Test
    public void testReset() {
        MessageCounter.getInstance().countUp(MessageCounter.ALL_HOSTS);
        MessageCounter.getInstance().reset(MessageCounter.ALL_HOSTS);
        assertEquals(MessageCounter.getInstance().getCount(MessageCounter.ALL_HOSTS), 0);
    }

    /**
     * Test of countUp method, of class MessageCounter.
     */
    @Test
    public void testCountUp() {
        int setCount = 4;
        for (int i = 0; i < setCount; i++) {
            MessageCounter.getInstance().countUp(MessageCounter.ALL_HOSTS);
        }
        assertEquals(MessageCounter.getInstance().getCount(MessageCounter.ALL_HOSTS), setCount);
    }

}