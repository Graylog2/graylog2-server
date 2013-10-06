/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
package org.graylog2.plugin;

import org.joda.time.DateTime;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class MessageTest {

    @Test
    public void testAddFieldDoesOnlyAcceptAlphanumericKeys() throws Exception {
        Message m = new Message("foo", "bar", new DateTime());
        m.addField("some_thing", "bar");
        assertEquals("bar", m.getField("some_thing"));

        m = new Message("foo", "bar", new DateTime());
        m.addField("some-thing", "bar");
        assertNull(m.getField("some-thing"));

        m = new Message("foo", "bar", new DateTime());
        m.addField("somethin$g", "bar");
        assertNull(m.getField("somethin$g"));

        m = new Message("foo", "bar", new DateTime());
        m.addField("someäthing", "bar");
        assertNull(m.getField("someäthing"));
    }

    @Test
    public void testAddFieldTrimsValue() throws Exception {
        Message m = new Message("foo", "bar", new DateTime());
        m.addField("something", " bar ");
        assertEquals("bar", m.getField("something"));

        m.addField("something2", " bar");
        assertEquals("bar", m.getField("something2"));

        m.addField("something3", "bar ");
        assertEquals("bar", m.getField("something3"));
    }

    @Test
    public void testAddFieldWorksWithIntegers() throws Exception {
        Message m = new Message("foo", "bar", new DateTime());
        m.addField("something", 3);
        assertEquals(3, m.getField("something"));
    }
}
