/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.logmessage;

import org.junit.Test;
import static org.junit.Assert.*;

public class LogMessageTest {

    @Test
    public void testIdGetsSet() {
        LogMessage lm = new LogMessage();
        assertNotNull(lm.getId());
        assertFalse(lm.getId().isEmpty());
    }

    @Test
    public void testIsCompleteSucceeds() {
        LogMessage lm = new LogMessage();
        lm.setShortMessage("foo");
        lm.setHost("bar");
        assertTrue(lm.isComplete());
    }

    @Test
    public void testIsCompleteFails() {
        LogMessage lm = new LogMessage();
        lm.setShortMessage("foo");
        lm.setHost(null);
        assertFalse(lm.isComplete());

        lm = new LogMessage();
        lm.setShortMessage("foo");
        lm.setHost("");
        assertFalse(lm.isComplete());

        lm = new LogMessage();
        lm.setShortMessage(null);
        lm.setHost("bar");
        assertFalse(lm.isComplete());

        lm = new LogMessage();
        lm.setShortMessage("");
        lm.setHost("bar");
        assertFalse(lm.isComplete());

        lm = new LogMessage();
        lm.setShortMessage("");
        lm.setHost("");
        assertFalse(lm.isComplete());

        lm = new LogMessage();
        lm.setShortMessage(null);
        lm.setHost(null);
        assertFalse(lm.isComplete());

        lm = new LogMessage();
        lm.setHost("bar");
        assertFalse(lm.isComplete());

        lm = new LogMessage();
        lm.setShortMessage("foo");
        assertFalse(lm.isComplete());
    }

    @Test
    public void testAddAdditionalData() {
        LogMessage lm = new LogMessage();
        lm.addAdditionalData("ohai", "thar");
        assertEquals("thar", lm.getAdditionalData().get("ohai"));
    }

    @Test
    public void testToString() {
        LogMessage lm = new LogMessage();
        lm.toString();
        lm.setHost("foo");
        lm.toString();

        // Fine if it does not crash.
    }

}