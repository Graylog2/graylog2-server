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

import org.graylog2.plugin.Message;
import com.beust.jcommander.internal.Maps;
import java.util.Map;
import java.util.HashMap;
import org.junit.Test;
import static org.junit.Assert.*;

public class LogMessageTest {

    @Test
    public void testIdGetsSet() {
        Message lm = new Message("foo", "bar", 0);
        assertNotNull(lm.getId());
        assertFalse(lm.getId().isEmpty());
    }

    @Test
    public void testIsCompleteSucceeds() {
        Message lm = new Message("foo", "bar", 0);
        assertTrue(lm.isComplete());
    }

    @Test
    public void testIsCompleteFails() {
        Message lm = new Message("foo", null, 0);
        assertFalse(lm.isComplete());

        lm = new Message("foo", "", 0);
        assertFalse(lm.isComplete());

        lm = new Message(null, "bar", 0);
        assertFalse(lm.isComplete());

        lm = new Message("", "bar", 0);
        assertFalse(lm.isComplete());

        lm = new Message("", "", 0);
        assertFalse(lm.isComplete());

        lm = new Message(null, null, 0);
        assertFalse(lm.isComplete());
    }

    @Test
    public void testAddField() {
        Message lm = new Message("foo", "bar", 0);
        lm.addField("ohai", "thar");
        assertEquals("thar", lm.getField("ohai"));
    }

    @Test
    public void testAddFieldsWithMap() {
        Message lm = new Message("foo", "bar", 0);
        lm.addField("ohai", "hai");

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("lol", "wut");
        map.put("aha", "pipes");

        lm.addFields(map);
        assertEquals(7, lm.getFields().size());
        assertEquals("wut", lm.getField("lol"));
        assertEquals("pipes", lm.getField("aha"));
        assertEquals("hai", lm.getField("ohai"));
    }

    
    @Test
    public void testAddFieldTrimsWhiteSpacesTabsAndStuffFromKeyAndValue() {
        Message lm = new Message("foo", "bar", 0);
        lm.addField(" one", "value_one");
        lm.addField(" two  ", "value_two");
        lm.addField("three ", "value_three");
        lm.addField("  four   ", "value_four_lol_tab");
        lm.addField("five", 5); // zomg integer
        
        assertEquals("value_one", lm.getField("one"));
        assertEquals("value_two", lm.getField("two"));
        assertEquals("value_three", lm.getField("three"));
        assertEquals("value_four_lol_tab", lm.getField("four"));
        assertEquals(5, lm.getField("five"));
    }
    
    @Test
    public void testAddFieldsTrimsWhiteSpacesTabsAndStuffFromKeyAndValueWhenInsertedAsMap() {
        Message lm = new Message("foo", "bar", 0);
        Map<String, Object> av = Maps.newHashMap();
        
        av.put(" one", "value_one");
        av.put(" two  ", "value_two");
        av.put("three ", "value_three");
        av.put("  four   ", "value_four_lol_tab");
        
        lm.addFields(av);
        
        assertEquals("value_one", lm.getField("one"));
        assertEquals("value_two", lm.getField("two"));
        assertEquals("value_three", lm.getField("three"));
        assertEquals("value_four_lol_tab", lm.getField("four"));
    }

    @Test
    public void testRemoveField() {
        Message lm = new Message("foo", "bar", 0);
        lm.addField("something", "foo");
        lm.addField("something_else", "bar");

        lm.removeField("something_else");

        assertEquals(5, lm.getFields().size());
        assertEquals("foo", lm.getField("something"));
    }

    @Test
    public void testRemoveFieldWithNonExistentKey() {
        Message lm = new Message("foo", "bar", 0);
        lm.addField("something", "foo");
        lm.addField("something_else", "bar");

        lm.removeField("LOLIDONTEXIST");

        assertEquals(6, lm.getFields().size());
    }
    
    @Test
    public void testRemoveFieldDoesNotDeleteReservedFields() {
        Message lm = new Message("foo", "bar", 9001);
        lm.removeField("source");
        lm.removeField("timestamp");
        lm.removeField("_id");

        assertTrue(lm.isComplete());
        assertEquals("foo", lm.getField("message"));
        assertEquals("bar", lm.getField("source"));
        assertEquals(9001.0, lm.getField("timestamp"));
        assertEquals(4, lm.getFields().size());
    }

    @Test
    public void testToString() {
        Message lm = new Message("foo", "bar", 0);
        lm.toString();
        // Fine if it does not crash.
    }

}