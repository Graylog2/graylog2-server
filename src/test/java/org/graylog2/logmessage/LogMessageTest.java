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

import com.beust.jcommander.internal.Maps;
import java.util.Map;
import java.util.HashMap;
import org.junit.Test;
import static org.junit.Assert.*;

public class LogMessageTest {

    @Test
    public void testIdGetsSet() {
        LogMessageImpl lm = new LogMessageImpl();
        assertNotNull(lm.getId());
        assertFalse(lm.getId().isEmpty());
    }

    @Test
    public void testIsCompleteSucceeds() {
        LogMessageImpl lm = new LogMessageImpl();
        lm.setShortMessage("foo");
        lm.setHost("bar");
        assertTrue(lm.isComplete());
    }

    @Test
    public void testIsCompleteFails() {
        LogMessageImpl lm = new LogMessageImpl();
        lm.setShortMessage("foo");
        lm.setHost(null);
        assertFalse(lm.isComplete());

        lm = new LogMessageImpl();
        lm.setShortMessage("foo");
        lm.setHost("");
        assertFalse(lm.isComplete());

        lm = new LogMessageImpl();
        lm.setShortMessage(null);
        lm.setHost("bar");
        assertFalse(lm.isComplete());

        lm = new LogMessageImpl();
        lm.setShortMessage("");
        lm.setHost("bar");
        assertFalse(lm.isComplete());

        lm = new LogMessageImpl();
        lm.setShortMessage("");
        lm.setHost("");
        assertFalse(lm.isComplete());

        lm = new LogMessageImpl();
        lm.setShortMessage(null);
        lm.setHost(null);
        assertFalse(lm.isComplete());

        lm = new LogMessageImpl();
        lm.setHost("bar");
        assertFalse(lm.isComplete());

        lm = new LogMessageImpl();
        lm.setShortMessage("foo");
        assertFalse(lm.isComplete());
    }

    @Test
    public void testAddAdditionalData() {
        LogMessageImpl lm = new LogMessageImpl();
        lm.addAdditionalData("_ohai", "thar");
        assertEquals("thar", lm.getAdditionalData().get("_ohai"));
    }

    @Test
    public void testAddAdditionalDataWithMap() {
        LogMessageImpl lm = new LogMessageImpl();
        lm.addAdditionalData("_ohai", "hai");

        Map<String, String> map = new HashMap<String, String>();

        map.put("_lol", "wut");
        map.put("_aha", "pipes");

        lm.addAdditionalData(map);
        assertEquals(3, lm.getAdditionalData().size());
        assertEquals("wut", lm.getAdditionalData().get("_lol"));
        assertEquals("pipes", lm.getAdditionalData().get("_aha"));
        assertEquals("hai", lm.getAdditionalData().get("_ohai"));
    }

    @Test
    public void testAddAdditionalDataAddsUnderscoreIfNotGiven() {
        LogMessageImpl lm = new LogMessageImpl();
        lm.addAdditionalData("ohai", "lol");
        lm.addAdditionalData("_wat", 9001);
        assertEquals(2, lm.getAdditionalData().size());
        assertEquals("lol", lm.getAdditionalData().get("_ohai"));
        assertEquals(9001, lm.getAdditionalData().get("_wat"));
    }
    
    @Test
    public void testAddAdditionalDataTrimsWhiteSpacesTabsAndStuffFromKeyAndValue() {
        LogMessageImpl lm = new LogMessageImpl();
        lm.addAdditionalData(" one", "value_one");
        lm.addAdditionalData(" two  ", "value_two");
        lm.addAdditionalData("three ", "value_three");
        lm.addAdditionalData("  four   ", "value_four_lol_tab");
        lm.addAdditionalData("five", 5); // zomg integer
        
        assertEquals("value_one", lm.getAdditionalData().get("_one"));
        assertEquals("value_two", lm.getAdditionalData().get("_two"));
        assertEquals("value_three", lm.getAdditionalData().get("_three"));
        assertEquals("value_four_lol_tab", lm.getAdditionalData().get("_four"));
        assertEquals(5, lm.getAdditionalData().get("_five"));
    }
    
    @Test
    public void testAddAdditionalDataTrimsWhiteSpacesTabsAndStuffFromKeyAndValueWhenInsertedAsMap() {
        LogMessageImpl lm = new LogMessageImpl();
        Map<String, String> av = Maps.newHashMap();
        
        av.put(" one", "value_one");
        av.put(" two  ", "value_two");
        av.put("three ", "value_three");
        av.put("  four   ", "value_four_lol_tab");
        
        lm.addAdditionalData(av);
        
        assertEquals("value_one", lm.getAdditionalData().get("_one"));
        assertEquals("value_two", lm.getAdditionalData().get("_two"));
        assertEquals("value_three", lm.getAdditionalData().get("_three"));
        assertEquals("value_four_lol_tab", lm.getAdditionalData().get("_four"));
    }

    @Test
    public void testRemoveAdditionalData() {
        LogMessageImpl lm = new LogMessageImpl();
        lm.addAdditionalData("_something", "foo");
        lm.addAdditionalData("_something_else", "bar");

        lm.removeAdditionalData("_something_else");

        assertEquals(1, lm.getAdditionalData().size());
        assertEquals("foo", lm.getAdditionalData().get("_something"));
    }

    @Test
    public void testRemoveAdditionalDataWithNonExistentKey() {
        LogMessageImpl lm = new LogMessageImpl();
        lm.addAdditionalData("_something", "foo");
        lm.addAdditionalData("_something_else", "bar");

        lm.removeAdditionalData("_LOLIDONTEXIST");

        assertEquals(2, lm.getAdditionalData().size());
    }

    @Test
    public void testToString() {
        LogMessageImpl lm = new LogMessageImpl();
        lm.toString();
        lm.setHost("foo");
        lm.toString();

        // Fine if it does not crash.
    }

}